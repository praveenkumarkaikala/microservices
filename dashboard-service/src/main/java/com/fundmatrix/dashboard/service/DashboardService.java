package com.fundmatrix.dashboard.service;

import com.fundmatrix.dashboard.client.AuthUserClient;
import com.fundmatrix.dashboard.client.DistributorCommissionClient;
import com.fundmatrix.dashboard.client.DistributorDto;
import com.fundmatrix.dashboard.client.FolioDto;
import com.fundmatrix.dashboard.client.FolioKycClient;
import com.fundmatrix.dashboard.client.FundCatalogClient;
import com.fundmatrix.dashboard.client.NavAccountingClient;
import com.fundmatrix.dashboard.client.NotificationClient;
import com.fundmatrix.dashboard.client.SchemeDto;
import com.fundmatrix.dashboard.client.TrailCommissionDto;
import com.fundmatrix.dashboard.client.TransactionClient;
import com.fundmatrix.dashboard.common.exception.BusinessException;
import com.fundmatrix.dashboard.dto.AdminStatsDto;
import com.fundmatrix.dashboard.dto.ComplianceSummaryDto;
import com.fundmatrix.dashboard.dto.DistributorDashboardDto;
import com.fundmatrix.dashboard.dto.FolioHoldingDto;
import com.fundmatrix.dashboard.dto.InvestorDashboardDto;
import com.fundmatrix.dashboard.dto.TransactionDto;
import com.fundmatrix.dashboard.security.CurrentUserService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated, role-specific dashboard read models - re-implemented as a pure BFF that owns
 * no data of its own. Every figure below is reassembled from the other services' existing
 * (monolith-derived) or newly-added internal read endpoints via Feign, exactly mirroring the
 * monolith's DashboardService/CommissionService.currentDistributorDashboard() aggregation
 * logic wherever a direct REST equivalent exists.
 */
@Service
public class DashboardService {

    private final FolioKycClient folioKycClient;
    private final TransactionClient transactionClient;
    private final NavAccountingClient navAccountingClient;
    private final NotificationClient notificationClient;
    private final AuthUserClient authUserClient;
    private final FundCatalogClient fundCatalogClient;
    private final DistributorCommissionClient distributorCommissionClient;
    private final CurrentUserService currentUser;

    public DashboardService(FolioKycClient folioKycClient,
                             TransactionClient transactionClient,
                             NavAccountingClient navAccountingClient,
                             NotificationClient notificationClient,
                             AuthUserClient authUserClient,
                             FundCatalogClient fundCatalogClient,
                             DistributorCommissionClient distributorCommissionClient,
                             CurrentUserService currentUser) {
        this.folioKycClient = folioKycClient;
        this.transactionClient = transactionClient;
        this.navAccountingClient = navAccountingClient;
        this.notificationClient = notificationClient;
        this.authUserClient = authUserClient;
        this.fundCatalogClient = fundCatalogClient;
        this.distributorCommissionClient = distributorCommissionClient;
        this.currentUser = currentUser;
    }

    /**
     * Investor Portal snapshot. folio-transaction-service's GET /folios and GET
     * /folios/{id}/holdings are both scoped to the current user (INVESTOR role forwards to
     * findByInvestor_Id), so this reconstructs the exact same figures the monolith computed
     * in-process, folio by folio.
     */
    public InvestorDashboardDto investorDashboard() {
        List<FolioDto> folios = folioKycClient.listFolios();

        BigDecimal invested = BigDecimal.ZERO;
        BigDecimal currentValue = BigDecimal.ZERO;
        BigDecimal unrealised = BigDecimal.ZERO;
        List<FolioHoldingDto> holdingDtos = new ArrayList<>();

        for (FolioDto folio : folios) {
            List<FolioHoldingDto> holdings = folioKycClient.folioHoldings(folio.id());
            for (FolioHoldingDto h : holdings) {
                invested = invested.add(nz(h.unitsHeld()).multiply(nz(h.averageCostNav())));
                currentValue = currentValue.add(nz(h.currentValue()));
                unrealised = unrealised.add(nz(h.unrealisedGainLoss()));
                holdingDtos.add(withResolvedNav(h));
            }
        }

        int activeSips = (int) transactionClient.sips().stream()
                .filter(s -> "ACTIVE".equals(s.status())).count();
        long unread = notificationClient.unreadCount().getOrDefault("unread", 0L);

        return new InvestorDashboardDto(folios.size(), money(invested), money(currentValue),
                money(unrealised), activeSips, unread, holdingDtos);
    }

    /**
     * Admin Console snapshot. GET /folios (default/non-INVESTOR branch of
     * FolioService.listForCurrentUser -> findAll()) returns every folio when called with an
     * ADMIN token, matching folioRepository.count() from the monolith exactly.
     *
     * Approximation: totalAum has no single owning-service SUM endpoint, so it is
     * reassembled here by summing every folio's holdings via GET /folios/{id}/holdings
     * (N+1 calls) instead of the monolith's single holdingRepository.findAll() SUM - the
     * resulting number is exact, just computed via a per-folio fan-out.
     */
    public AdminStatsDto adminStats() {
        long totalUsers = authUserClient.listUsers(null).size();

        List<SchemeDto> schemes = fundCatalogClient.listSchemes();
        long totalSchemes = schemes.size();
        long activeSchemes = schemes.stream().filter(s -> "ACTIVE".equals(s.status())).count();

        List<FolioDto> folios = folioKycClient.listFolios();
        long totalFolios = folios.size();

        long totalDistributors = distributorCommissionClient.listDistributors().size();

        long pending = transactionClient.transactionQueue().size();

        BigDecimal totalAum = BigDecimal.ZERO;
        for (FolioDto folio : folios) {
            for (FolioHoldingDto h : folioKycClient.folioHoldings(folio.id())) {
                totalAum = totalAum.add(nz(h.currentValue()));
            }
        }

        return new AdminStatsDto(totalUsers, totalSchemes, activeSchemes, totalFolios,
                totalDistributors, pending, money(totalAum));
    }

    /**
     * Compliance Portal snapshot. GET /kyc?status= and GET /transactions/flagged are both
     * global (not user-scoped) monolith-derived routes, so these figures are exact - no
     * approximation needed.
     */
    public ComplianceSummaryDto complianceSummary() {
        long compliant = folioKycClient.listByStatus("COMPLIANT").size();
        long nonCompliant = folioKycClient.listByStatus("NON_COMPLIANT").size();
        long pendingKyc = folioKycClient.listByStatus("PENDING").size();
        long expired = folioKycClient.listByStatus("EXPIRED").size();

        List<TransactionDto> flagged = transactionClient.flaggedTransactions();

        return new ComplianceSummaryDto(compliant, nonCompliant, pendingKyc, expired,
                flagged.size(), flagged);
    }

    /**
     * Distributor Console snapshot. No internal contract endpoint resolves "distributor
     * record for this login user id" directly, so this lists all distributors (existing
     * monolith-derived GET /distributors route) and matches by userId client-side - the same
     * lookup the monolith did via distributorRepository.findByUser_Id(currentUser.getId()),
     * just performed here instead of inside distributor-commission-service.
     */
    public DistributorDashboardDto distributorDashboard() {
    	System.out.println("calling");
        Long userId = currentUser.getId();
       DistributorDto distributor = distributorCommissionClient.listDistributors().stream().filter(dis->userId==dis.userId()).findFirst().orElseThrow(()->  new BusinessException("Not found"));
        System.out.println(distributor);
        BigDecimal aum = folioKycClient.aumForDistributor(distributor.userId(), null);
        Long folioCount = folioKycClient.folioCountForDistributor(distributor.userId());

        List<TrailCommissionDto> commissions =
                distributorCommissionClient.commissionsByDistributor(distributor.id());
        BigDecimal paid = commissions.stream().filter(c -> "PAID".equals(c.status()))
                .map(c -> nz(c.commissionAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = commissions.stream().filter(c -> !"PAID".equals(c.status()))
                .map(c -> nz(c.commissionAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DistributorDashboardDto(distributor.id(), distributor.name(), distributor.arnNumber(),
                folioCount == null ? 0 : folioCount.intValue(), money(aum), money(paid), money(pending));
       
    }

    // ----------------------------------------------------------------- helpers

    /** Fills in latestNav from nav-accounting-service when the upstream holding didn't have one yet. */
    private FolioHoldingDto withResolvedNav(FolioHoldingDto h) {
        if (h.latestNav() != null || h.optionId() == null) {
            return h;
        }
        BigDecimal resolved = navAccountingClient.latestNavOrNull(h.optionId());
        return new FolioHoldingDto(h.id(), h.folioId(), h.folioNumber(), h.schemeId(), h.schemeName(),
                h.optionId(), h.optionType(), h.unitsHeld(), h.averageCostNav(), resolved,
                h.currentValue(), h.unrealisedGainLoss(), h.lastUpdated());
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal money(BigDecimal value) {
        return nz(value).setScale(2, RoundingMode.HALF_UP);
    }
}
