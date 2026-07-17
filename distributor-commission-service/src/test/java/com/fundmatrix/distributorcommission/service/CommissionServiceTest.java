package com.fundmatrix.distributorcommission.service;

import com.fundmatrix.distributorcommission.client.FolioKycClient;
import com.fundmatrix.distributorcommission.client.FundCatalogClient;
import com.fundmatrix.distributorcommission.client.NotificationClient;
import com.fundmatrix.distributorcommission.client.SchemeDto;
import com.fundmatrix.distributorcommission.common.exception.BusinessException;
import com.fundmatrix.distributorcommission.common.exception.ResourceNotFoundException;
import com.fundmatrix.distributorcommission.domain.Distributor;
import com.fundmatrix.distributorcommission.domain.TrailCommission;
import com.fundmatrix.distributorcommission.domain.enums.CommissionModel;
import com.fundmatrix.distributorcommission.domain.enums.CommissionStatus;
import com.fundmatrix.distributorcommission.domain.enums.DistributorStatus;
import com.fundmatrix.distributorcommission.domain.enums.Role;
import com.fundmatrix.distributorcommission.dto.ComputeCommissionRequest;
import com.fundmatrix.distributorcommission.dto.DistributorDashboardDto;
import com.fundmatrix.distributorcommission.dto.TrailCommissionDto;
import com.fundmatrix.distributorcommission.repository.DistributorRepository;
import com.fundmatrix.distributorcommission.repository.TrailCommissionRepository;
import com.fundmatrix.distributorcommission.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommissionServiceTest {

    @Mock
    private TrailCommissionRepository commissionRepository;
    @Mock
    private DistributorRepository distributorRepository;
    @Mock
    private FolioKycClient folioTransactionClient;
    @Mock
    private FundCatalogClient fundCatalogClient;
    @Mock
    private NotificationClient notificationClient;
    @Mock
    private AuditService auditService;
    @Mock
    private CurrentUserService currentUser;

    @InjectMocks
    private CommissionService commissionService;

    private Distributor distributor;
    private SchemeDto scheme;

    @BeforeEach
    void setUp() {
        distributor = Distributor.builder()
                .name("Acme Distributors")
                .arnNumber("ARN123")
                .commissionModel(CommissionModel.TRAIL)
                .status(DistributorStatus.ACTIVE)
                .userId(42L)
                .build();
        distributor.setId(1L);

        scheme = new SchemeDto(5L, "Bluechip Equity Fund", "BEF001");

        lenient().when(fundCatalogClient.getScheme(5L)).thenReturn(scheme);
    }

    @Test
    void compute_usesAumFromFeignClient() {
        when(distributorRepository.findById(1L)).thenReturn(Optional.of(distributor));
        when(folioTransactionClient.aumForDistributor(1L, 5L)).thenReturn(new BigDecimal("1200000.00"));
        when(commissionRepository.findByDistributor_IdAndSchemeIdAndPeriod(1L, 5L, "2026-06"))
                .thenReturn(Optional.empty());
        when(commissionRepository.save(any(TrailCommission.class)))
                .thenAnswer(inv -> {
                    TrailCommission tc = inv.getArgument(0);
                    tc.setId(100L);
                    return tc;
                });

        ComputeCommissionRequest req = new ComputeCommissionRequest(1L, 5L, "2026-06", new BigDecimal("1.2"));
        TrailCommissionDto dto = commissionService.compute(req);

        // annual = 1200000 * 1.2 / 100 = 14400.00 ; monthly = 14400 / 12 = 1200.00
        assertThat(dto.aumManaged()).isEqualByComparingTo("1200000.00");
        assertThat(dto.commissionAmount()).isEqualByComparingTo("1200.00");
        assertThat(dto.status()).isEqualTo(CommissionStatus.COMPUTED);
        assertThat(dto.schemeName()).isEqualTo("Bluechip Equity Fund");
        verify(folioTransactionClient).aumForDistributor(1L, 5L);
        verify(auditService).record(eq("COMMISSION_COMPUTE"), eq("TrailCommission"), any(), any());
    }

    @Test
    void compute_distributorNotFound_throwsResourceNotFound() {
        when(distributorRepository.findById(99L)).thenReturn(Optional.empty());
        ComputeCommissionRequest req = new ComputeCommissionRequest(99L, 5L, "2026-06", new BigDecimal("1.2"));

        assertThatThrownBy(() -> commissionService.compute(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void approve_onlyAllowedFromComputedStatus() {
        TrailCommission tc = TrailCommission.builder()
                .distributor(distributor).schemeId(5L).period("2026-06")
                .status(CommissionStatus.APPROVED).build();
        tc.setId(10L);
        when(commissionRepository.findById(10L)).thenReturn(Optional.of(tc));

        assertThatThrownBy(() -> commissionService.approve(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("COMPUTED");
    }

    @Test
    void approve_fromComputed_succeeds() {
        TrailCommission tc = TrailCommission.builder()
                .distributor(distributor).schemeId(5L).period("2026-06")
                .status(CommissionStatus.COMPUTED).build();
        tc.setId(10L);
        when(commissionRepository.findById(10L)).thenReturn(Optional.of(tc));
        when(commissionRepository.save(tc)).thenReturn(tc);

        TrailCommissionDto dto = commissionService.approve(10L);

        assertThat(dto.status()).isEqualTo(CommissionStatus.APPROVED);
    }

    @Test
    void pay_onlyAllowedFromApprovedStatus_throwsBusinessException() {
        TrailCommission tc = TrailCommission.builder()
                .distributor(distributor).schemeId(5L).period("2026-06")
                .status(CommissionStatus.COMPUTED).build();
        tc.setId(11L);
        when(commissionRepository.findById(11L)).thenReturn(Optional.of(tc));

        assertThatThrownBy(() -> commissionService.pay(11L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("APPROVED");
        verify(notificationClient, never()).notify(any());
    }

    @Test
    void pay_fromApproved_paysAndNotifiesLinkedUser() {
        TrailCommission tc = TrailCommission.builder()
                .distributor(distributor).schemeId(5L).period("2026-06")
                .commissionAmount(new BigDecimal("1200.00"))
                .status(CommissionStatus.APPROVED).build();
        tc.setId(12L);
        when(commissionRepository.findById(12L)).thenReturn(Optional.of(tc));
        when(commissionRepository.save(tc)).thenReturn(tc);

        TrailCommissionDto dto = commissionService.pay(12L);

        assertThat(dto.status()).isEqualTo(CommissionStatus.PAID);
        assertThat(dto.payoutDate()).isNotNull();
        verify(notificationClient).notify(any());
        verify(auditService).record(eq("COMMISSION_PAY"), eq("TrailCommission"), any(), any());
    }

    @Test
    void currentDistributorDashboard_aggregatesFromMockedFeignResponses() {
        when(currentUser.getId()).thenReturn(42L);
        when(distributorRepository.findByUserId(42L)).thenReturn(Optional.of(distributor));
        when(folioTransactionClient.aumForDistributor(eq(1L), isNull())).thenReturn(new BigDecimal("500000.00"));
        when(folioTransactionClient.folioCountForDistributor(1L)).thenReturn(7L);

        TrailCommission paidOne = TrailCommission.builder()
                .distributor(distributor).schemeId(5L).period("2026-05")
                .commissionAmount(new BigDecimal("100.00")).status(CommissionStatus.PAID).build();
        TrailCommission pendingOne = TrailCommission.builder()
                .distributor(distributor).schemeId(5L).period("2026-06")
                .commissionAmount(new BigDecimal("150.00")).status(CommissionStatus.APPROVED).build();
        when(commissionRepository.findByDistributor_IdOrderByPeriodDesc(1L))
                .thenReturn(List.of(pendingOne, paidOne));

        DistributorDashboardDto dashboard = commissionService.currentDistributorDashboard();

        assertThat(dashboard.distributorId()).isEqualTo(1L);
        assertThat(dashboard.clientFolioCount()).isEqualTo(7);
        assertThat(dashboard.aumManaged()).isEqualByComparingTo("500000.00");
        assertThat(dashboard.commissionEarnedToDate()).isEqualByComparingTo("100.00");
        assertThat(dashboard.commissionPending()).isEqualByComparingTo("150.00");
    }

    @Test
    void currentDistributorDashboard_noLinkedDistributor_throwsBusinessException() {
        when(currentUser.getId()).thenReturn(999L);
        when(distributorRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commissionService.currentDistributorDashboard())
                .isInstanceOf(BusinessException.class);
    }
}
