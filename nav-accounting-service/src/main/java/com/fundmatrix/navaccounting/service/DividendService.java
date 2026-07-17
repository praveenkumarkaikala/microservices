package com.fundmatrix.navaccounting.service;

import com.fundmatrix.navaccounting.client.CreditUnitsRequest;
import com.fundmatrix.navaccounting.client.FolioDto;
import com.fundmatrix.navaccounting.client.FolioKycClient;
import com.fundmatrix.navaccounting.client.FundCatalogClient;
import com.fundmatrix.navaccounting.client.HoldingDto;
import com.fundmatrix.navaccounting.client.SchemeOptionDto;
import com.fundmatrix.navaccounting.common.Calc;
import com.fundmatrix.navaccounting.common.FeignSupport;
import com.fundmatrix.navaccounting.common.exception.BusinessException;
import com.fundmatrix.navaccounting.common.exception.ResourceNotFoundException;
import com.fundmatrix.navaccounting.domain.DividendDeclaration;
import com.fundmatrix.navaccounting.domain.InvestorDividendEntitlement;
import com.fundmatrix.navaccounting.domain.enums.DividendStatus;
import com.fundmatrix.navaccounting.domain.enums.EntitlementStatus;
import com.fundmatrix.navaccounting.domain.enums.NotificationCategory;
import com.fundmatrix.navaccounting.domain.enums.OptionType;
import com.fundmatrix.navaccounting.domain.enums.PayoutMode;
import com.fundmatrix.navaccounting.dto.CreateDividendRequest;
import com.fundmatrix.navaccounting.dto.DividendDeclarationDto;
import com.fundmatrix.navaccounting.dto.EntitlementDto;
import com.fundmatrix.navaccounting.repository.DividendDeclarationRepository;
import com.fundmatrix.navaccounting.repository.InvestorDividendEntitlementRepository;
import com.fundmatrix.navaccounting.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dividend declaration, entitlement computation and payout/reinvestment processing.
 *
 * <p>Cross-service rewrites vs the monolith:
 * <ul>
 *   <li>{@code SchemeOptionRepository} lookups now go through {@link FundCatalogClient};
 *       schemeId/schemeName/optionType are snapshotted onto {@link DividendDeclaration} at
 *       declare() time.</li>
 *   <li>{@code holdingRepository.findByOption_Id(...)} -> {@link FolioKycClient#getHoldingsByOption}.</li>
 *   <li>{@code holdingService.creditUnits(...)} -> {@link FolioKycClient#creditUnits}.</li>
 *   <li>{@code holdingService.latestNavOrNull(...)} stays a LOCAL call (delegates to
 *       {@link NavService#latestNavOrNull}) since NavRecord is owned by this same service - no
 *       Feign hop needed for an intra-service lookup.</li>
 *   <li>{@code myEntitlements()}: {@code InvestorDividendEntitlement.folio} is now a plain
 *       {@code folioId}, so the derived-join query the monolith used
 *       ({@code findByFolio_Investor_IdOrderByIdDesc}) is no longer possible. Instead the
 *       caller's own folio ids are resolved first via {@link FolioKycClient#myFolios()}
 *       (which relies on the forwarded Authorization header identifying the investor), then
 *       entitlements are filtered by {@code findByFolioIdInOrderByIdDesc(folioIds)}. See
 *       {@link InvestorDividendEntitlementRepository} for the same rationale.</li>
 * </ul>
 */
@Service
public class DividendService {

    private final DividendDeclarationRepository declarationRepository;
    private final InvestorDividendEntitlementRepository entitlementRepository;
    private final FundCatalogClient fundCatalogClient;
    private final FolioKycClient folioTransactionClient;
    private final NavService navService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;
    private final BigDecimal tdsRate;

    public DividendService(DividendDeclarationRepository declarationRepository,
                           InvestorDividendEntitlementRepository entitlementRepository,
                           FundCatalogClient fundCatalogClient, FolioKycClient folioTransactionClient,
                           NavService navService, NotificationService notificationService,
                           AuditService auditService, CurrentUserService currentUser, Mapper mapper,
                           @Value("${fundmatrix.tax.dividend-tds-rate}") BigDecimal tdsRate) {
        this.declarationRepository = declarationRepository;
        this.entitlementRepository = entitlementRepository;
        this.fundCatalogClient = fundCatalogClient;
        this.folioTransactionClient = folioTransactionClient;
        this.navService = navService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
        this.tdsRate = tdsRate;
    }

    @Transactional
    public DividendDeclarationDto declare(CreateDividendRequest req) {
        SchemeOptionDto option = FeignSupport.call(() -> fundCatalogClient.getOption(req.optionId()),
                "SchemeOption", req.optionId());
        OptionType optionType = OptionType.valueOf(option.optionType());
        if (optionType == OptionType.GROWTH) {
            throw new BusinessException("Dividends cannot be declared on a Growth option");
        }
        DividendDeclaration declaration = DividendDeclaration.builder()
                .schemeId(option.schemeId()).schemeName(option.schemeName())
                .optionId(option.id()).optionType(optionType)
                .recordDate(req.recordDate())
                .dividendPerUnit(Calc.rate(req.dividendPerUnit()))
                .totalDistributionAmount(BigDecimal.ZERO)
                .declaredById(currentUser.getId())
                .status(DividendStatus.DECLARED)
                .build();
        declaration = declarationRepository.save(declaration);
        auditService.record("DIVIDEND_DECLARE", "DividendDeclaration", declaration.getId(),
                "Declared " + req.dividendPerUnit() + "/unit on option " + option.id());
        return toDto(declaration);
    }

    @Transactional
    public List<EntitlementDto> computeEntitlements(Long declarationId) {
        DividendDeclaration declaration = require(declarationId);
        if (declaration.getStatus() == DividendStatus.PROCESSED
                || declaration.getStatus() == DividendStatus.CANCELLED) {
            throw new BusinessException("Cannot recompute a " + declaration.getStatus() + " declaration");
        }
        // Recompute from scratch.
        entitlementRepository.deleteAll(entitlementRepository.findByDeclarationId(declarationId));

        BigDecimal dpu = declaration.getDividendPerUnit();
        boolean reinvest = declaration.getOptionType() == OptionType.DIVIDEND_REINVESTMENT;
        BigDecimal totalGross = BigDecimal.ZERO;

        List<HoldingDto> holdings = FeignSupport.call(
                () -> folioTransactionClient.getHoldingsByOption(declaration.getOptionId()),
                "Holdings", declaration.getOptionId());
        for (HoldingDto h : holdings) {
            BigDecimal units = Calc.nz(h.unitsHeld());
            if (units.signum() <= 0) {
                continue;
            }
            BigDecimal gross = Calc.amountFor(units, dpu);
            BigDecimal tax = Calc.percentOf(gross, tdsRate);
            BigDecimal net = Calc.money(gross.subtract(tax));
            InvestorDividendEntitlement e = InvestorDividendEntitlement.builder()
                    .declaration(declaration).folioId(h.folioId()).investorId(h.investorId())
                    .unitsOnRecordDate(Calc.units(units))
                    .grossDividend(gross).taxDeducted(tax).netDividend(net)
                    .payoutMode(reinvest ? PayoutMode.REINVESTMENT : PayoutMode.BANK_CREDIT)
                    .status(EntitlementStatus.COMPUTED)
                    .build();
            entitlementRepository.save(e);
            totalGross = totalGross.add(gross);
        }
        declaration.setTotalDistributionAmount(Calc.money(totalGross));
        declarationRepository.save(declaration);
        auditService.record("DIVIDEND_COMPUTE", "DividendDeclaration", declarationId,
                "Computed entitlements, total gross " + totalGross);
        return getEntitlements(declarationId);
    }

    @Transactional
    public DividendDeclarationDto approve(Long declarationId) {
        DividendDeclaration declaration = require(declarationId);
        if (declaration.getStatus() != DividendStatus.DECLARED) {
            throw new BusinessException("Only DECLARED dividends can be approved");
        }
        declaration.setStatus(DividendStatus.APPROVED);
        auditService.record("DIVIDEND_APPROVE", "DividendDeclaration", declarationId, "Approved");
        return toDto(declarationRepository.save(declaration));
    }

    @Transactional
    public DividendDeclarationDto process(Long declarationId) {
        DividendDeclaration declaration = require(declarationId);
        if (declaration.getStatus() != DividendStatus.APPROVED) {
            throw new BusinessException("Only APPROVED dividends can be processed");
        }
        List<InvestorDividendEntitlement> entitlements =
                entitlementRepository.findByDeclarationId(declarationId);
        if (entitlements.isEmpty()) {
            throw new BusinessException("No entitlements computed for this declaration");
        }
        // Intra-service lookup - NavRecord is owned here, so no Feign call is needed.
        BigDecimal nav = navService.latestNavOrNull(declaration.getOptionId());

        for (InvestorDividendEntitlement e : entitlements) {
            if (e.getStatus() != EntitlementStatus.COMPUTED) {
                continue;
            }
            if (e.getPayoutMode() == PayoutMode.REINVESTMENT && nav != null && nav.signum() > 0) {
                BigDecimal addUnits = Calc.unitsFor(e.getNetDividend(), nav);
                CreditUnitsRequest creditReq = new CreditUnitsRequest(e.getFolioId(), declaration.getSchemeId(),
                        declaration.getOptionId(), addUnits, e.getNetDividend(), nav);
                FeignSupport.call(() -> folioTransactionClient.creditUnits(creditReq), "Holding", e.getFolioId());
                e.setStatus(EntitlementStatus.REINVESTED);
                notificationService.notify(e.getInvestorId(), NotificationCategory.DIVIDEND,
                        "Dividend of " + e.getNetDividend() + " reinvested as " + addUnits
                                + " units in " + declaration.getSchemeName());
            } else {
                e.setStatus(EntitlementStatus.DISBURSED);
                notificationService.notify(e.getInvestorId(), NotificationCategory.DIVIDEND,
                        "Dividend of " + e.getNetDividend() + " credited to your registered bank account ("
                                + declaration.getSchemeName() + ")");
            }
            entitlementRepository.save(e);
        }
        declaration.setStatus(DividendStatus.PROCESSED);
        auditService.record("DIVIDEND_PROCESS", "DividendDeclaration", declarationId,
                "Processed " + entitlements.size() + " entitlements");
        return toDto(declarationRepository.save(declaration));
    }

    @Transactional
    public DividendDeclarationDto cancel(Long declarationId) {
        DividendDeclaration declaration = require(declarationId);
        if (declaration.getStatus() == DividendStatus.PROCESSED) {
            throw new BusinessException("Processed dividends cannot be cancelled");
        }
        declaration.setStatus(DividendStatus.CANCELLED);
        auditService.record("DIVIDEND_CANCEL", "DividendDeclaration", declarationId, "Cancelled");
        return toDto(declarationRepository.save(declaration));
    }

    @Transactional(readOnly = true)
    public List<DividendDeclarationDto> list(DividendStatus status) {
        List<DividendDeclaration> declarations = (status == null)
                ? declarationRepository.findAll() : declarationRepository.findByStatus(status);
        return declarations.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<EntitlementDto> getEntitlements(Long declarationId) {
        return entitlementRepository.findByDeclarationId(declarationId).stream()
                .map(mapper::toEntitlementDto).toList();
    }

    @Transactional(readOnly = true)
    public List<EntitlementDto> myEntitlements() {
        List<FolioDto> folios = FeignSupport.call(folioTransactionClient::myFolios, "Folio", currentUser.getId());
        List<Long> folioIds = folios.stream().map(FolioDto::id).toList();
        if (folioIds.isEmpty()) {
            return List.of();
        }
        return entitlementRepository.findByFolioIdInOrderByIdDesc(folioIds).stream()
                .map(mapper::toEntitlementDto).toList();
    }

    private DividendDeclaration require(Long id) {
        return declarationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("DividendDeclaration", id));
    }

    private DividendDeclarationDto toDto(DividendDeclaration d) {
        return mapper.toDividendDto(d, entitlementRepository.countByDeclarationId(d.getId()));
    }
}
