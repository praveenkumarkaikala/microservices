package com.fundmatrix.navaccounting.service;

import com.fundmatrix.navaccounting.client.FolioKycClient;
import com.fundmatrix.navaccounting.client.FundCatalogClient;
import com.fundmatrix.navaccounting.client.HoldingDto;
import com.fundmatrix.navaccounting.client.SchemeDto;
import com.fundmatrix.navaccounting.client.SchemeOptionDto;
import com.fundmatrix.navaccounting.common.Calc;
import com.fundmatrix.navaccounting.common.FeignSupport;
import com.fundmatrix.navaccounting.common.exception.BusinessException;
import com.fundmatrix.navaccounting.common.exception.ResourceNotFoundException;
import com.fundmatrix.navaccounting.domain.FundExpenseAccrual;
import com.fundmatrix.navaccounting.domain.NavRecord;
import com.fundmatrix.navaccounting.domain.enums.ExpenseStatus;
import com.fundmatrix.navaccounting.domain.enums.NavStatus;
import com.fundmatrix.navaccounting.domain.enums.NotificationCategory;
import com.fundmatrix.navaccounting.dto.AumSummaryDto;
import com.fundmatrix.navaccounting.dto.NavRecordDto;
import com.fundmatrix.navaccounting.dto.SaveNavRequest;
import com.fundmatrix.navaccounting.repository.FundExpenseAccrualRepository;
import com.fundmatrix.navaccounting.repository.NavRecordRepository;
import com.fundmatrix.navaccounting.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * NAV capture, publication (with holding revaluation) and AUM reporting.
 *
 * <p>Cross-service rewrites vs the monolith:
 * <ul>
 *   <li>Holdings-by-option + revaluation, previously via {@code HoldingRepository}/
 *       {@code HoldingService}, now go through {@link FolioKycClient}.</li>
 *   <li>Scheme/option lookups, previously JPA relations, now go through
 *       {@link FundCatalogClient} and are snapshotted onto {@link NavRecord} (schemeName,
 *       optionType) so reads don't need a Feign round trip.</li>
 *   <li>{@code aumSummary()} can no longer iterate "all schemes" (fund-catalog-service has no
 *       list-all endpoint in the Feign contract) nor re-aggregate holdings scheme-wide (no such
 *       endpoint either - folio-transaction-service's contract only aggregates AUM by
 *       distributor). It instead iterates the distinct scheme ids that have at least one NAV
 *       record captured in THIS service, and reports the totals already persisted on each
 *       scheme's latest {@link NavRecord} (totalAum/totalUnitsOutstanding are populated at
 *       publish() time). This is a deliberate, documented simplification.</li>
 * </ul>
 */
@Service
public class NavService {

    private final NavRecordRepository navRepository;
    private final FundExpenseAccrualRepository accrualRepository;
    private final FundCatalogClient fundCatalogClient;
    private final FolioKycClient folioTransactionClient;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public NavService(NavRecordRepository navRepository, FundExpenseAccrualRepository accrualRepository,
                      FundCatalogClient fundCatalogClient, FolioKycClient folioTransactionClient,
                      NotificationService notificationService, AuditService auditService,
                      CurrentUserService currentUser, Mapper mapper) {
        this.navRepository = navRepository;
        this.accrualRepository = accrualRepository;
        this.fundCatalogClient = fundCatalogClient;
        this.folioTransactionClient = folioTransactionClient;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional
    public NavRecordDto saveNavInput(SaveNavRequest req) {
        SchemeOptionDto option = FeignSupport.call(() -> fundCatalogClient.getOption(req.optionId()),
                "SchemeOption", req.optionId());

        NavRecord nav = navRepository.findByOptionIdAndNavDate(req.optionId(), req.navDate())
                .orElseGet(() -> NavRecord.builder()
                        .schemeId(option.schemeId()).schemeName(option.schemeName())
                        .optionId(option.id()).optionType(option.optionType())
                        .navDate(req.navDate())
                        .status(NavStatus.PROVISIONAL).build());

        nav.setNavValue(Calc.rate(req.navValue()));
        nav.setTotalAum(req.totalAum());
        nav.setTotalUnitsOutstanding(req.totalUnitsOutstanding());
        if (nav.getStatus() == NavStatus.PUBLISHED) {
            nav.setStatus(NavStatus.REVISED);
        }
        nav = navRepository.save(nav);
        auditService.record("NAV_INPUT", "NavRecord", nav.getId(),
                "Captured NAV " + nav.getNavValue() + " for option " + nav.getOptionId());
        return mapper.toNavDto(nav);
    }

    @Transactional
    public NavRecordDto publish(Long navId) {
        NavRecord nav = navRepository.findById(navId)
                .orElseThrow(() -> ResourceNotFoundException.of("NavRecord", navId));
        if (nav.getStatus() == NavStatus.PUBLISHED) {
            throw new BusinessException("NAV is already published");
        }
        final Long optionId = nav.getOptionId();

        // Derive AUM figures from holdings when not supplied.
        List<HoldingDto> holdings = FeignSupport.call(
                () -> folioTransactionClient.getHoldingsByOption(optionId), "Holdings", optionId);
        BigDecimal totalUnits = holdings.stream().map(h -> Calc.nz(h.unitsHeld()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Apply the scheme's un-applied accrued expenses to this NAV (real expense impact):
        // NAV is reduced by expense-per-unit, then those accruals are marked APPLIED so they
        // are deducted only once. (Phase-1: applied on the first option NAV published that day.)
        applyAccruedExpenses(nav, totalUnits);

        if (nav.getTotalUnitsOutstanding() == null) {
            nav.setTotalUnitsOutstanding(Calc.units(totalUnits));
        }
        if (nav.getTotalAum() == null) {
            nav.setTotalAum(Calc.amountFor(totalUnits, nav.getNavValue()));
        }
        nav.setStatus(NavStatus.PUBLISHED);
        nav.setPublishedById(currentUser.getId());
        final NavRecord savedNav = navRepository.save(nav);
        nav = savedNav;

        Integer revalued = FeignSupport.call(
                () -> folioTransactionClient.revalueOption(optionId, Map.of("navValue", savedNav.getNavValue())),
                "Holdings", optionId);

        // NAV publication alerts to affected investors.
        Set<Long> notified = new HashSet<>();
        for (HoldingDto h : holdings) {
            if (h.investorId() != null && notified.add(h.investorId())) {
                notificationService.notify(h.investorId(), NotificationCategory.NAV,
                        "NAV for " + nav.getSchemeName() + " published at "
                                + nav.getNavValue() + " (" + nav.getNavDate() + ")");
            }
        }
        auditService.record("NAV_PUBLISH", "NavRecord", nav.getId(),
                "Published NAV " + nav.getNavValue() + "; revalued " + (revalued == null ? 0 : revalued) + " holdings");
        return mapper.toNavDto(nav);
    }

    /** Reduces the NAV by the per-unit value of the scheme's un-applied accruals, then marks them APPLIED. */
    private void applyAccruedExpenses(NavRecord nav, BigDecimal totalUnits) {
        if (totalUnits == null || totalUnits.signum() <= 0) {
            return;
        }
        List<FundExpenseAccrual> accrued =
                accrualRepository.findBySchemeIdAndStatus(nav.getSchemeId(), ExpenseStatus.ACCRUED);
        BigDecimal totalExpense = accrued.stream()
                .map(a -> Calc.nz(a.getAccrualAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalExpense.signum() <= 0) {
            return;
        }
        BigDecimal perUnit = totalExpense.divide(totalUnits, Calc.UNIT_SCALE, Calc.RM);
        BigDecimal reduced = Calc.rate(nav.getNavValue().subtract(perUnit).max(BigDecimal.ZERO));
        nav.setNavValue(reduced);
        accrued.forEach(a -> a.setStatus(ExpenseStatus.APPLIED));
        accrualRepository.saveAll(accrued);
        auditService.record("NAV_EXPENSE_APPLIED", "NavRecord", nav.getId(),
                "Deducted expenses " + Calc.money(totalExpense) + " (" + perUnit + "/unit); NAV -> " + reduced);
    }

    @Transactional(readOnly = true)
    public List<NavRecordDto> listByScheme(Long schemeId) {
        return navRepository.findBySchemeIdOrderByNavDateDesc(schemeId)
                .stream().map(mapper::toNavDto).toList();
    }

    @Transactional(readOnly = true)
    public List<NavRecordDto> listByOption(Long optionId) {
        return navRepository.findByOptionIdOrderByNavDateDesc(optionId)
                .stream().map(mapper::toNavDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AumSummaryDto> aumSummary() {
        List<Long> schemeIds = navRepository.findDistinctSchemeIds();
        List<AumSummaryDto> out = new ArrayList<>();
        for (Long schemeId : schemeIds) {
            SchemeDto scheme = FeignSupport.call(() -> fundCatalogClient.getScheme(schemeId), "FundScheme", schemeId);
            NavRecord latest = navRepository.findTopBySchemeIdOrderByNavDateDesc(schemeId).orElse(null);
            BigDecimal aum = latest != null ? latest.getTotalAum() : null;
            BigDecimal units = latest != null ? latest.getTotalUnitsOutstanding() : null;
            BigDecimal latestNav = latest != null ? latest.getNavValue() : null;
            out.add(new AumSummaryDto(schemeId, scheme.schemeName(), scheme.schemeCode(), scheme.category(),
                    latestNav, Calc.money(Calc.nz(aum)), Calc.units(Calc.nz(units))));
        }
        return out;
    }

    /** Backing implementation for the internal GET /nav/published/{optionId} endpoint. */
    @Transactional(readOnly = true)
    public BigDecimal requirePublishedNav(Long optionId) {
        return navRepository.findTopByOptionIdAndStatusOrderByNavDateDesc(optionId, NavStatus.PUBLISHED)
                .map(NavRecord::getNavValue)
                .orElseThrow(() -> new BusinessException("No published NAV available for option " + optionId));
    }

    /** Backing implementation for the internal GET /nav/latest/{optionId} endpoint. */
    @Transactional(readOnly = true)
    public BigDecimal latestNavOrNull(Long optionId) {
        return navRepository.findTopByOptionIdOrderByNavDateDesc(optionId)
                .map(NavRecord::getNavValue)
                .orElse(null);
    }
}
