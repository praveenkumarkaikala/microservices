package com.fundmatrix.distributorcommission.service;

import com.fundmatrix.distributorcommission.client.FolioKycClient;
import com.fundmatrix.distributorcommission.client.FundCatalogClient;
import com.fundmatrix.distributorcommission.client.NotificationClient;
import com.fundmatrix.distributorcommission.client.NotificationRequest;
import com.fundmatrix.distributorcommission.client.SchemeDto;
import com.fundmatrix.distributorcommission.common.Calc;
import com.fundmatrix.distributorcommission.common.exception.BusinessException;
import com.fundmatrix.distributorcommission.common.exception.ResourceNotFoundException;
import com.fundmatrix.distributorcommission.domain.Distributor;
import com.fundmatrix.distributorcommission.domain.TrailCommission;
import com.fundmatrix.distributorcommission.domain.enums.CommissionStatus;
import com.fundmatrix.distributorcommission.dto.ComputeCommissionRequest;
import com.fundmatrix.distributorcommission.dto.DistributorDashboardDto;
import com.fundmatrix.distributorcommission.dto.TrailCommissionDto;
import com.fundmatrix.distributorcommission.repository.DistributorRepository;
import com.fundmatrix.distributorcommission.repository.TrailCommissionRepository;
import com.fundmatrix.distributorcommission.security.CurrentUserService;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Trail commission computation, approval and payout, plus the distributor AUM dashboard. */
@Service
public class CommissionService {

    private static final Logger log = LoggerFactory.getLogger(CommissionService.class);
    private static final BigDecimal MONTHS = BigDecimal.valueOf(12);

    private final TrailCommissionRepository commissionRepository;
    private final DistributorRepository distributorRepository;
    private final FolioKycClient folioTransactionClient;
    private final FundCatalogClient fundCatalogClient;
    private final NotificationClient notificationClient;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public CommissionService(TrailCommissionRepository commissionRepository,
                             DistributorRepository distributorRepository,
                             FolioKycClient folioTransactionClient,
                             FundCatalogClient fundCatalogClient,
                             NotificationClient notificationClient,
                             AuditService auditService, CurrentUserService currentUser) {
        this.commissionRepository = commissionRepository;
        this.distributorRepository = distributorRepository;
        this.folioTransactionClient = folioTransactionClient;
        this.fundCatalogClient = fundCatalogClient;
        this.notificationClient = notificationClient;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional
    public TrailCommissionDto compute(ComputeCommissionRequest req) {
        Distributor distributor = distributorRepository.findById(req.distributorId())
                .orElseThrow(() -> ResourceNotFoundException.of("Distributor", req.distributorId()));
        SchemeDto scheme = requireScheme(req.schemeId());

        BigDecimal aum = Calc.money(folioTransactionClient.aumForDistributor(distributor.getId(), scheme.id()));
        // One billing month of an annualised trail rate on the period-end AUM.
        BigDecimal annual = Calc.percentOf(aum, req.trailRate());
        BigDecimal commission = annual.divide(MONTHS, Calc.AMOUNT_SCALE, Calc.RM);

        TrailCommission tc = commissionRepository
                .findByDistributor_IdAndSchemeIdAndPeriod(distributor.getId(), scheme.id(), req.period())
                .orElseGet(() -> TrailCommission.builder()
                        .distributor(distributor).schemeId(scheme.id()).period(req.period()).build());
        tc.setAumManaged(aum);
        tc.setTrailRate(Calc.rate(req.trailRate()));
        tc.setCommissionAmount(commission);
        tc.setStatus(CommissionStatus.COMPUTED);
        tc = commissionRepository.save(tc);

        auditService.record("COMMISSION_COMPUTE", "TrailCommission", tc.getId(),
                "AUM " + aum + " @ " + req.trailRate() + "% -> " + commission + " for " + req.period());
        return toCommissionDto(tc, scheme.schemeName());
    }

    @Transactional
    public TrailCommissionDto approve(Long id) {
        TrailCommission tc = require(id);
        if (tc.getStatus() != CommissionStatus.COMPUTED) {
            throw new BusinessException("Only COMPUTED commissions can be approved");
        }
        tc.setStatus(CommissionStatus.APPROVED);
        auditService.record("COMMISSION_APPROVE", "TrailCommission", id, "Approved");
        return toCommissionDto(commissionRepository.save(tc));
    }

    @Transactional
    public TrailCommissionDto pay(Long id) {
        TrailCommission tc = require(id);
        if (tc.getStatus() != CommissionStatus.APPROVED) {
            throw new BusinessException("Only APPROVED commissions can be paid");
        }
        tc.setStatus(CommissionStatus.PAID);
        tc.setPayoutDate(LocalDate.now());
        commissionRepository.save(tc);
        String schemeName = schemeName(tc.getSchemeId());
        if (tc.getDistributor().getUserId() != null) {
            try {
                notificationClient.notify(new NotificationRequest(tc.getDistributor().getUserId(), "COMMISSION",
                        "Trail commission of " + tc.getCommissionAmount() + " for " + tc.getPeriod()
                                + " (" + schemeName + ") has been paid"));
            } catch (FeignException ex) {
                log.warn("Unable to notify distributor {} of commission payout: {}",
                        tc.getDistributor().getId(), ex.getMessage());
            }
        }
        auditService.record("COMMISSION_PAY", "TrailCommission", id,
                "Paid " + tc.getCommissionAmount());
        return toCommissionDto(tc, schemeName);
    }

    @Transactional(readOnly = true)
    public List<TrailCommissionDto> listByDistributor(Long distributorId) {
        return commissionRepository.findByDistributor_IdOrderByPeriodDesc(distributorId)
                .stream().map(this::toCommissionDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TrailCommissionDto> listForCurrentDistributor() {
        Distributor distributor = requireForCurrentUser();
        return listByDistributor(distributor.getId());
    }

    @Transactional(readOnly = true)
    public DistributorDashboardDto currentDistributorDashboard() {
        Distributor d = requireForCurrentUser();
        BigDecimal aum = Calc.money(folioTransactionClient.aumForDistributor(d.getId(), null));
        Long folioCountLong = folioTransactionClient.folioCountForDistributor(d.getId());
        int folioCount = folioCountLong == null ? 0 : folioCountLong.intValue();
        List<TrailCommission> commissions = commissionRepository.findByDistributor_IdOrderByPeriodDesc(d.getId());
        BigDecimal paid = commissions.stream().filter(c -> c.getStatus() == CommissionStatus.PAID)
                .map(c -> Calc.nz(c.getCommissionAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = commissions.stream().filter(c -> c.getStatus() != CommissionStatus.PAID)
                .map(c -> Calc.nz(c.getCommissionAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DistributorDashboardDto(d.getId(), d.getName(), d.getArnNumber(), folioCount,
                aum, Calc.money(paid), Calc.money(pending));
    }

    private Distributor requireForCurrentUser() {
        return distributorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new BusinessException("No distributor profile is linked to your account"));
    }

    private TrailCommission require(Long id) {
        return commissionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("TrailCommission", id));
    }

    private SchemeDto requireScheme(Long schemeId) {
        SchemeDto scheme;
        try {
            scheme = fundCatalogClient.getScheme(schemeId);
        } catch (FeignException.NotFound ex) {
            throw ResourceNotFoundException.of("FundScheme", schemeId);
        } catch (FeignException ex) {
            throw new BusinessException("Unable to look up scheme " + schemeId + ": " + ex.getMessage());
        }
        if (scheme == null) {
            throw ResourceNotFoundException.of("FundScheme", schemeId);
        }
        return scheme;
    }

    private String schemeName(Long schemeId) {
        try {
            SchemeDto scheme = fundCatalogClient.getScheme(schemeId);
            return scheme != null ? scheme.schemeName() : "scheme #" + schemeId;
        } catch (FeignException ex) {
            log.warn("Unable to fetch scheme {} name: {}", schemeId, ex.getMessage());
            return "scheme #" + schemeId;
        }
    }

    private TrailCommissionDto toCommissionDto(TrailCommission c) {
        return toCommissionDto(c, schemeName(c.getSchemeId()));
    }

    private TrailCommissionDto toCommissionDto(TrailCommission c, String schemeName) {
        return new TrailCommissionDto(c.getId(), c.getDistributor().getId(), c.getDistributor().getName(),
                c.getSchemeId(), schemeName, c.getPeriod(), c.getAumManaged(),
                c.getTrailRate(), c.getCommissionAmount(), c.getPayoutDate(), c.getStatus());
    }
}
