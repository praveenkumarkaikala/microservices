package com.fundmatrix.transaction.service;

import com.fundmatrix.transaction.client.FundCatalogClient;
import com.fundmatrix.transaction.common.exception.BusinessException;
import com.fundmatrix.transaction.common.exception.ResourceNotFoundException;
import com.fundmatrix.transaction.domain.SwpMandate;
import com.fundmatrix.transaction.domain.enums.NotificationCategory;
import com.fundmatrix.transaction.domain.enums.Role;
import com.fundmatrix.transaction.domain.enums.SipFrequency;
import com.fundmatrix.transaction.domain.enums.SipStatus;
import com.fundmatrix.transaction.dto.CreateSwpRequest;
import com.fundmatrix.transaction.dto.FolioDto;
import com.fundmatrix.transaction.dto.SchemeDto;
import com.fundmatrix.transaction.dto.SchemeOptionDto;
import com.fundmatrix.transaction.dto.SwpMandateDto;
import com.fundmatrix.transaction.dto.UpdateSwpRequest;
import com.fundmatrix.transaction.repository.SwpMandateRepository;
import com.fundmatrix.transaction.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Systematic Withdrawal Plan mandate management and instalment execution.
 * Scheme/option data (min SWP amount, scheme name) is fetched via {@link FundCatalogClient}.
 * Folio access-control/status checks go through {@link FolioAccessService}.
 */
@Service
public class SwpService {

    private final SwpMandateRepository swpRepository;
    private final FundCatalogClient fundCatalogClient;
    private final FolioAccessService folioAccessService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public SwpService(SwpMandateRepository swpRepository, FundCatalogClient fundCatalogClient,
                      FolioAccessService folioAccessService, TransactionService transactionService,
                      NotificationService notificationService, AuditService auditService,
                      CurrentUserService currentUser, Mapper mapper) {
        this.swpRepository = swpRepository;
        this.fundCatalogClient = fundCatalogClient;
        this.folioAccessService = folioAccessService;
        this.transactionService = transactionService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional
    public SwpMandateDto create(CreateSwpRequest req) {
        FolioDto folio = folioAccessService.loadAccessible(req.folioId());
        if (!"ACTIVE".equals(folio.status())) {
            throw new BusinessException("Folio is not active");
        }
        SchemeOptionDto option = fundCatalogClient.getOption(req.optionId());
        if (option == null) {
            throw ResourceNotFoundException.of("SchemeOption", req.optionId());
        }
        SchemeDto scheme = fundCatalogClient.getScheme(option.schemeId());

        var minSwp = scheme != null ? scheme.minSwpAmount() : null;
        if (minSwp != null && req.amount().compareTo(minSwp) < 0) {
            throw new BusinessException("Minimum SWP amount for " + option.schemeName()
                    + " is " + minSwp);
        }

        SwpMandate mandate = SwpMandate.builder()
                .folioId(folio.id()).folioNumber(folio.folioNumber()).investorId(folio.investorId())
                .distributorId(folio.distributorId())
                .schemeId(option.schemeId()).optionId(option.id())
                .amount(req.amount())
                .frequency(req.frequency())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .instalmentCount(req.instalmentCount())
                .instalmentsExecuted(0)
                .nextInstalmentDate(req.startDate())
                .status(SipStatus.ACTIVE)
                .build();
        mandate = swpRepository.save(mandate);
        mandate.setMandateRef(String.format("SWP%06d", mandate.getId()));
        mandate = swpRepository.save(mandate);

        notificationService.notify(folio.investorId(), NotificationCategory.SIP,
                "SWP mandate " + mandate.getMandateRef() + " for " + req.amount() + " ("
                        + req.frequency() + ") registered");
        auditService.record("SWP_CREATE", "SwpMandate", mandate.getId(),
                "SWP " + req.amount() + " " + req.frequency() + " folio " + folio.folioNumber());
        return mapper.toSwpDto(mandate, option.schemeName());
    }

    @Transactional(readOnly = true)
    public List<SwpMandateDto> listForCurrentUser() {
        List<SwpMandate> mandates = (currentUser.getRole() == Role.INVESTOR)
                ? swpRepository.findByInvestorId(currentUser.getId())
                : swpRepository.findAll();
        return mandates.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public SwpMandateDto get(Long id) {
        return toDto(require(id));
    }

    @Transactional
    public SwpMandateDto update(Long id, UpdateSwpRequest req) {
        SwpMandate mandate = require(id);
        if (mandate.getStatus() == SipStatus.COMPLETED || mandate.getStatus() == SipStatus.CANCELLED) {
            throw new BusinessException("Cannot edit a " + mandate.getStatus() + " mandate");
        }
        if (req.amount() != null) mandate.setAmount(req.amount());
        if (req.frequency() != null) mandate.setFrequency(req.frequency());
        if (req.endDate() != null) mandate.setEndDate(req.endDate());
        if (req.instalmentCount() != null) mandate.setInstalmentCount(req.instalmentCount());
        auditService.record("SWP_UPDATE", "SwpMandate", id, "Mandate updated");
        return toDto(swpRepository.save(mandate));
    }

    @Transactional
    public SwpMandateDto changeStatus(Long id, SipStatus status) {
        SwpMandate mandate = require(id);
        mandate.setStatus(status);
        auditService.record("SWP_STATUS", "SwpMandate", id, "Status set to " + status);
        return toDto(swpRepository.save(mandate));
    }

    /** Executes one SWP instalment: redeems units worth the fixed amount, then advances the schedule. */
    @Transactional
    public SwpMandateDto process(Long id) {
        SwpMandate mandate = require(id);
        if (mandate.getStatus() != SipStatus.ACTIVE) {
            throw new BusinessException("SWP mandate is " + mandate.getStatus() + "; cannot process instalment");
        }

        transactionService.placeAndAllotSwpInstalment(mandate);

        int executed = mandate.getInstalmentsExecuted() + 1;
        mandate.setInstalmentsExecuted(executed);
        mandate.setNextInstalmentDate(nextDate(mandate));
        if (mandate.getInstalmentCount() != null && executed >= mandate.getInstalmentCount()) {
            mandate.setStatus(SipStatus.COMPLETED);
        } else if (mandate.getEndDate() != null && mandate.getNextInstalmentDate().isAfter(mandate.getEndDate())) {
            mandate.setStatus(SipStatus.COMPLETED);
        }
        mandate = swpRepository.save(mandate);

        notificationService.notify(mandate.getInvestorId(), NotificationCategory.SIP,
                "SWP instalment " + executed + " for " + mandate.getMandateRef() + " processed");
        auditService.record("SWP_INSTALMENT", "SwpMandate", mandate.getId(),
                "Instalment " + executed + " executed");
        return toDto(mandate);
    }

    private SwpMandate require(Long id) {
        return swpRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("SwpMandate", id));
    }

    private LocalDate nextDate(SwpMandate mandate) {
        LocalDate base = mandate.getNextInstalmentDate() != null
                ? mandate.getNextInstalmentDate() : LocalDate.now();
        return mandate.getFrequency() == SipFrequency.QUARTERLY ? base.plusMonths(3) : base.plusMonths(1);
    }

    private SwpMandateDto toDto(SwpMandate mandate) {
        String schemeName = safeSchemeName(mandate.getOptionId());
        return mapper.toSwpDto(mandate, schemeName);
    }

    private String safeSchemeName(Long optionId) {
        try {
            SchemeOptionDto option = fundCatalogClient.getOption(optionId);
            return option != null ? option.schemeName() : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
