package com.fundmatrix.navaccounting.service;

import com.fundmatrix.navaccounting.client.FundCatalogClient;
import com.fundmatrix.navaccounting.client.SchemeDto;
import com.fundmatrix.navaccounting.common.Calc;
import com.fundmatrix.navaccounting.common.FeignSupport;
import com.fundmatrix.navaccounting.common.exception.BusinessException;
import com.fundmatrix.navaccounting.domain.FundExpenseAccrual;
import com.fundmatrix.navaccounting.domain.NavRecord;
import com.fundmatrix.navaccounting.domain.enums.ExpenseStatus;
import com.fundmatrix.navaccounting.domain.enums.ExpenseType;
import com.fundmatrix.navaccounting.dto.CreateAccrualRequest;
import com.fundmatrix.navaccounting.dto.ExpenseAccrualDto;
import com.fundmatrix.navaccounting.dto.ExpenseComplianceDto;
import com.fundmatrix.navaccounting.repository.FundExpenseAccrualRepository;
import com.fundmatrix.navaccounting.repository.NavRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Fund-level expense accrual booking and reversal.
 *
 * <p>Cross-service rewrite: {@code FundSchemeRepository} lookups now go through
 * {@link FundCatalogClient}. The monolith's daily-accrual default
 * ({@code amount == null -> percentOf(currentSchemeAum, annualisedRate) / 365}) used
 * {@code FolioHoldingRepository.sumCurrentValueByScheme(schemeId)} for "current scheme AUM" -
 * folio-transaction-service's Feign contract has no scheme-wide AUM aggregate (only
 * per-distributor), so this uses the scheme's own latest published {@link NavRecord#getTotalAum()}
 * (owned locally, refreshed on every {@code NavService.publish()}) as the pragmatic substitute.
 */
@Service
public class AccrualService {

    private static final BigDecimal DAYS_IN_YEAR = BigDecimal.valueOf(365);

    private final FundExpenseAccrualRepository accrualRepository;
    private final NavRecordRepository navRepository;
    private final FundCatalogClient fundCatalogClient;
    private final AuditService auditService;
    private final Mapper mapper;

    public AccrualService(FundExpenseAccrualRepository accrualRepository, NavRecordRepository navRepository,
                          FundCatalogClient fundCatalogClient, AuditService auditService, Mapper mapper) {
        this.accrualRepository = accrualRepository;
        this.navRepository = navRepository;
        this.fundCatalogClient = fundCatalogClient;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Transactional
    public ExpenseAccrualDto create(CreateAccrualRequest req) {
        SchemeDto scheme = FeignSupport.call(() -> fundCatalogClient.getScheme(req.schemeId()),
                "FundScheme", req.schemeId());

        BigDecimal amount = req.accrualAmount();
        if (amount == null) {
            // Daily accrual on scheme AUM at the annualised rate.
            BigDecimal aum = Calc.money(currentSchemeAum(scheme.id()));
            amount = Calc.percentOf(aum, req.annualisedRate()).divide(DAYS_IN_YEAR, Calc.AMOUNT_SCALE, Calc.RM);
        }

        FundExpenseAccrual accrual = FundExpenseAccrual.builder()
                .schemeId(scheme.id()).schemeName(scheme.schemeName())
                .expenseType(req.expenseType())
                .annualisedRate(Calc.rate(req.annualisedRate()))
                .accrualAmount(Calc.money(amount))
                .accrualDate(req.accrualDate() != null ? req.accrualDate() : LocalDate.now())
                .status(ExpenseStatus.ACCRUED)
                .build();
        accrual = accrualRepository.save(accrual);
        auditService.record("ACCRUAL_CREATE", "FundExpenseAccrual", accrual.getId(),
                req.expenseType() + " accrual " + accrual.getAccrualAmount() + " for " + scheme.schemeName());
        return mapper.toAccrualDto(accrual);
    }

    private BigDecimal currentSchemeAum(Long schemeId) {
        return navRepository.findTopBySchemeIdOrderByNavDateDesc(schemeId)
                .map(NavRecord::getTotalAum)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<ExpenseAccrualDto> listByScheme(Long schemeId) {
        return accrualRepository.findBySchemeIdOrderByAccrualDateDesc(schemeId)
                .stream().map(mapper::toAccrualDto).toList();
    }

    @Transactional
    public ExpenseAccrualDto reverse(Long id, String reason) {
        FundExpenseAccrual accrual = accrualRepository.findById(id)
                .orElseThrow(() -> com.fundmatrix.navaccounting.common.exception.ResourceNotFoundException.of(
                        "FundExpenseAccrual", id));
        if (accrual.getStatus() == ExpenseStatus.REVERSED) {
            throw new BusinessException("Accrual is already reversed");
        }
        accrual.setStatus(ExpenseStatus.REVERSED);
        accrual.setReversalReason(reason);
        auditService.record("ACCRUAL_REVERSE", "FundExpenseAccrual", id, "Reversed: " + reason);
        return mapper.toAccrualDto(accrualRepository.save(accrual));
    }

    /** Expense-ratio compliance: charged rate (sum of latest annualised rate per type) vs the TER limit. */
    @Transactional(readOnly = true)
    public ExpenseComplianceDto compliance(Long schemeId) {
        SchemeDto scheme = FeignSupport.call(() -> fundCatalogClient.getScheme(schemeId), "FundScheme", schemeId);
        Map<ExpenseType, BigDecimal> perType = new EnumMap<>(ExpenseType.class);
        accrualRepository.findBySchemeIdOrderByAccrualDateDesc(schemeId).stream()
                .filter(a -> a.getStatus() != ExpenseStatus.REVERSED)
                .forEach(a -> perType.merge(a.getExpenseType(), Calc.nz(a.getAnnualisedRate()), BigDecimal::max));
        BigDecimal charged = perType.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal limit = scheme.expenseRatio();
        BigDecimal util = null;
        String status;
        if (limit == null || limit.signum() <= 0) {
            status = "NO_LIMIT";
        } else {
            util = charged.multiply(BigDecimal.valueOf(100)).divide(limit, 2, RoundingMode.HALF_UP);
            status = charged.compareTo(limit) > 0 ? "BREACH"
                    : (util.compareTo(BigDecimal.valueOf(80)) >= 0 ? "WARN" : "OK");
        }
        return new ExpenseComplianceDto(scheme.id(), scheme.schemeName(), limit,
                Calc.rate(charged), util, status);
    }
}
