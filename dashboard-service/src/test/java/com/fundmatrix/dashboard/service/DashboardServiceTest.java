package com.fundmatrix.dashboard.service;

import com.fundmatrix.dashboard.client.AuthUserClient;
import com.fundmatrix.dashboard.client.DistributorCommissionClient;
import com.fundmatrix.dashboard.client.DistributorDto;
import com.fundmatrix.dashboard.client.FolioDto;
import com.fundmatrix.dashboard.client.FolioKycClient;
import com.fundmatrix.dashboard.client.TransactionClient;
import com.fundmatrix.dashboard.client.FundCatalogClient;
import com.fundmatrix.dashboard.client.KycRecordDto;
import com.fundmatrix.dashboard.client.NavAccountingClient;
import com.fundmatrix.dashboard.client.NotificationClient;
import com.fundmatrix.dashboard.client.SchemeDto;
import com.fundmatrix.dashboard.client.SipMandateDto;
import com.fundmatrix.dashboard.client.TrailCommissionDto;
import com.fundmatrix.dashboard.dto.AdminStatsDto;
import com.fundmatrix.dashboard.dto.ComplianceSummaryDto;
import com.fundmatrix.dashboard.dto.FolioHoldingDto;
import com.fundmatrix.dashboard.dto.InvestorDashboardDto;
import com.fundmatrix.dashboard.dto.TransactionDto;
import com.fundmatrix.dashboard.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private FolioKycClient folioKycClient;
    @Mock private TransactionClient transactionClient;
    @Mock private NavAccountingClient navAccountingClient;
    @Mock private NotificationClient notificationClient;
    @Mock private AuthUserClient authUserClient;
    @Mock private FundCatalogClient fundCatalogClient;
    @Mock private DistributorCommissionClient distributorCommissionClient;
    @Mock private CurrentUserService currentUser;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(folioKycClient, transactionClient, navAccountingClient,
                notificationClient, authUserClient, fundCatalogClient, distributorCommissionClient,
                currentUser);
    }

    @Test
    void investorDashboard_aggregatesFoliosHoldingsSipsAndNotifications() {
        FolioDto folio1 = new FolioDto(1L, "F001", 10L, null, "ACTIVE");
        FolioDto folio2 = new FolioDto(2L, "F002", 10L, null, "ACTIVE");
        when(folioKycClient.listFolios()).thenReturn(List.of(folio1, folio2));

        FolioHoldingDto h1 = new FolioHoldingDto(100L, 1L, "F001", 5L, "Scheme A", 50L, "GROWTH",
                new BigDecimal("100"), new BigDecimal("10.00"), new BigDecimal("12.00"),
                new BigDecimal("1200.00"), new BigDecimal("200.00"), null);
        FolioHoldingDto h2 = new FolioHoldingDto(101L, 2L, "F002", 6L, "Scheme B", 51L, "DIVIDEND",
                new BigDecimal("50"), new BigDecimal("20.00"), null,
                new BigDecimal("1100.00"), new BigDecimal("100.00"), null);
        when(folioKycClient.folioHoldings(1L)).thenReturn(List.of(h1));
        when(folioKycClient.folioHoldings(2L)).thenReturn(List.of(h2));
        when(navAccountingClient.latestNavOrNull(51L)).thenReturn(new BigDecimal("22.00"));

        when(transactionClient.sips()).thenReturn(List.of(
                new SipMandateDto(1L, 1L, "ACTIVE"),
                new SipMandateDto(2L, 2L, "PAUSED")));
        when(notificationClient.unreadCount()).thenReturn(Map.of("unread", 4L));

        InvestorDashboardDto result = dashboardService.investorDashboard();

        assertThat(result.folioCount()).isEqualTo(2);
        // invested = 100*10.00 + 50*20.00 = 1000 + 1000 = 2000.00
        assertThat(result.totalInvested()).isEqualByComparingTo("2000.00");
        // currentValue = 1200 + 1100 = 2300.00
        assertThat(result.currentValue()).isEqualByComparingTo("2300.00");
        // unrealised = 200 + 100 = 300.00
        assertThat(result.unrealisedGainLoss()).isEqualByComparingTo("300.00");
        assertThat(result.activeSipCount()).isEqualTo(1);
        assertThat(result.unreadNotifications()).isEqualTo(4L);
        assertThat(result.holdings()).hasSize(2);
        // holding with a null latestNav gets backfilled from nav-accounting-service
        assertThat(result.holdings().get(1).latestNav()).isEqualByComparingTo("22.00");
    }

    @Test
    void adminStats_aggregatesCountsAndSumsAumAcrossAllFolios() {
        when(authUserClient.listUsers(any())).thenReturn(List.of(
                new com.fundmatrix.dashboard.client.UserDto(1L, "A", "a@x.com", "INVESTOR", "ACTIVE"),
                new com.fundmatrix.dashboard.client.UserDto(2L, "B", "b@x.com", "ADMIN", "ACTIVE")));

        when(fundCatalogClient.listSchemes()).thenReturn(List.of(
                new SchemeDto(1L, "Scheme A", "SA", "ACTIVE"),
                new SchemeDto(2L, "Scheme B", "SB", "CLOSED")));

        FolioDto folio1 = new FolioDto(1L, "F001", 10L, null, "ACTIVE");
        when(folioKycClient.listFolios()).thenReturn(List.of(folio1));
        FolioHoldingDto h1 = new FolioHoldingDto(100L, 1L, "F001", 5L, "Scheme A", 50L, "GROWTH",
                new BigDecimal("100"), new BigDecimal("10.00"), new BigDecimal("12.00"),
                new BigDecimal("1200.00"), new BigDecimal("200.00"), null);
        when(folioKycClient.folioHoldings(1L)).thenReturn(List.of(h1));

        when(distributorCommissionClient.listDistributors()).thenReturn(List.of(
                new DistributorDto(1L, "Dist A", "ARN1", "ACTIVE", 20L)));

        when(transactionClient.transactionQueue()).thenReturn(List.of(
                sampleTxn(1L), sampleTxn(2L)));

        AdminStatsDto result = dashboardService.adminStats();

        assertThat(result.totalUsers()).isEqualTo(2);
        assertThat(result.totalSchemes()).isEqualTo(2);
        assertThat(result.activeSchemes()).isEqualTo(1);
        assertThat(result.totalFolios()).isEqualTo(1);
        assertThat(result.totalDistributors()).isEqualTo(1);
        assertThat(result.pendingTransactions()).isEqualTo(2);
        assertThat(result.totalAum()).isEqualByComparingTo("1200.00");
    }

    @Test
    void complianceSummary_aggregatesKycCountsAndFlaggedTransactions() {
        when(folioKycClient.listByStatus("COMPLIANT")).thenReturn(List.of(
                new KycRecordDto(1L, 10L, "COMPLIANT"), new KycRecordDto(2L, 11L, "COMPLIANT")));
        when(folioKycClient.listByStatus("NON_COMPLIANT")).thenReturn(List.of(
                new KycRecordDto(3L, 12L, "NON_COMPLIANT")));
        when(folioKycClient.listByStatus("PENDING")).thenReturn(List.of());
        when(folioKycClient.listByStatus("EXPIRED")).thenReturn(List.of());

        TransactionDto flagged = sampleTxn(9L);
        when(transactionClient.flaggedTransactions()).thenReturn(List.of(flagged));

        ComplianceSummaryDto result = dashboardService.complianceSummary();

        assertThat(result.compliantCount()).isEqualTo(2);
        assertThat(result.nonCompliantCount()).isEqualTo(1);
        assertThat(result.pendingCount()).isEqualTo(0);
        assertThat(result.expiredCount()).isEqualTo(0);
        assertThat(result.flaggedTransactionCount()).isEqualTo(1);
        assertThat(result.flaggedTransactions()).containsExactly(flagged);
    }

    @Test
    void distributorDashboard_resolvesDistributorByUserIdAndSumsCommissions() {
        when(currentUser.getId()).thenReturn(20L);
        when(distributorCommissionClient.listDistributors()).thenReturn(List.of(
                new DistributorDto(1L, "Dist A", "ARN1", "ACTIVE", 99L),
                new DistributorDto(2L, "Dist B", "ARN2", "ACTIVE", 20L)));
        when(folioKycClient.aumForDistributor(2L, null)).thenReturn(new BigDecimal("50000.00"));
        when(folioKycClient.folioCountForDistributor(2L)).thenReturn(7L);
        when(distributorCommissionClient.commissionsByDistributor(2L)).thenReturn(List.of(
                new TrailCommissionDto(1L, 2L, new BigDecimal("500.00"), "PAID"),
                new TrailCommissionDto(2L, 2L, new BigDecimal("300.00"), "COMPUTED")));

        var result = dashboardService.distributorDashboard();

        assertThat(result.distributorId()).isEqualTo(2L);
        assertThat(result.distributorName()).isEqualTo("Dist B");
        assertThat(result.clientFolioCount()).isEqualTo(7);
        assertThat(result.aumManaged()).isEqualByComparingTo("50000.00");
        assertThat(result.commissionEarnedToDate()).isEqualByComparingTo("500.00");
        assertThat(result.commissionPending()).isEqualByComparingTo("300.00");
    }

    private static TransactionDto sampleTxn(Long id) {
        return new TransactionDto(id, "TXN" + id, 1L, "F001", 5L, "Scheme A", 50L, "GROWTH",
                "SUBSCRIPTION", new BigDecimal("600000.00"), new BigDecimal("100"),
                new BigDecimal("12.00"), java.time.Instant.now(), "BEFORE_CUTOFF",
                "RECEIVED", BigDecimal.ZERO, null);
    }
}
