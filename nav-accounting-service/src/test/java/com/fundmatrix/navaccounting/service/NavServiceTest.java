package com.fundmatrix.navaccounting.service;

import com.fundmatrix.navaccounting.client.FolioKycClient;
import com.fundmatrix.navaccounting.client.FundCatalogClient;
import com.fundmatrix.navaccounting.client.HoldingDto;
import com.fundmatrix.navaccounting.domain.FundExpenseAccrual;
import com.fundmatrix.navaccounting.domain.NavRecord;
import com.fundmatrix.navaccounting.domain.enums.ExpenseStatus;
import com.fundmatrix.navaccounting.domain.enums.ExpenseType;
import com.fundmatrix.navaccounting.domain.enums.NavStatus;
import com.fundmatrix.navaccounting.dto.NavRecordDto;
import com.fundmatrix.navaccounting.repository.FundExpenseAccrualRepository;
import com.fundmatrix.navaccounting.repository.NavRecordRepository;
import com.fundmatrix.navaccounting.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NavServiceTest {

    @Mock
    private NavRecordRepository navRepository;
    @Mock
    private FundExpenseAccrualRepository accrualRepository;
    @Mock
    private FundCatalogClient fundCatalogClient;
    @Mock
    private FolioKycClient folioTransactionClient;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditService auditService;
    @Mock
    private CurrentUserService currentUser;

    private NavService navService;

    @BeforeEach
    void setUp() {
        navService = new NavService(navRepository, accrualRepository, fundCatalogClient,
                folioTransactionClient, notificationService, auditService, currentUser, new Mapper());
    }

    @Test
    void publishRevaluesViaFeignClientAndAppliesAccruedExpenses() {
        NavRecord nav = NavRecord.builder()
                .schemeId(10L).schemeName("Blue Chip Fund").optionId(20L).optionType("GROWTH")
                .navDate(LocalDate.of(2026, 7, 15))
                .navValue(new BigDecimal("100.0000"))
                .status(NavStatus.PROVISIONAL)
                .build();
        nav.setId(1L);

        when(navRepository.findById(1L)).thenReturn(Optional.of(nav));

        HoldingDto h1 = new HoldingDto(1L, 100L, 10L, 20L, new BigDecimal("1000.0000"),
                new BigDecimal("95.0000"), null, null, 500L);
        HoldingDto h2 = new HoldingDto(2L, 101L, 10L, 20L, new BigDecimal("500.0000"),
                new BigDecimal("95.0000"), null, null, 501L);
        when(folioTransactionClient.getHoldingsByOption(20L)).thenReturn(List.of(h1, h2));

        // Total units on the option = 1500. One accrued expense of 1500 -> 1.00/unit.
        FundExpenseAccrual accrual = FundExpenseAccrual.builder()
                .schemeId(10L).schemeName("Blue Chip Fund")
                .expenseType(ExpenseType.MANAGEMENT_FEE)
                .accrualAmount(new BigDecimal("1500.00"))
                .accrualDate(LocalDate.of(2026, 7, 15))
                .status(ExpenseStatus.ACCRUED)
                .build();
        when(accrualRepository.findBySchemeIdAndStatus(10L, ExpenseStatus.ACCRUED)).thenReturn(List.of(accrual));

        when(currentUser.getId()).thenReturn(999L);
        when(navRepository.save(any(NavRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(folioTransactionClient.revalueOption(eq(20L), any())).thenReturn(2);

        NavRecordDto dto = navService.publish(1L);

        // NAV reduced by 1500/1500 = 1.00 per unit: 100.0000 -> 99.0000
        assertThat(dto.navValue()).isEqualByComparingTo("99.0000");
        assertThat(dto.status()).isEqualTo(NavStatus.PUBLISHED);
        assertThat(dto.publishedById()).isEqualTo(999L);

        // Accrual marked APPLIED exactly once.
        assertThat(accrual.getStatus()).isEqualTo(ExpenseStatus.APPLIED);
        verify(accrualRepository).saveAll(List.of(accrual));

        // Revaluation delegated to the Feign client with the (already expense-adjusted) NAV.
        ArgumentCaptor<Map<String, BigDecimal>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(folioTransactionClient).revalueOption(eq(20L), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue().get("navValue")).isEqualByComparingTo("99.0000");

        // Both distinct investors on the option get notified once.
        verify(notificationService, times(1)).notify(eq(500L), any(), any());
        verify(notificationService, times(1)).notify(eq(501L), any(), any());
    }

    @Test
    void publishingAlreadyPublishedNavThrows() {
        NavRecord nav = NavRecord.builder().schemeId(1L).optionId(2L)
                .navValue(BigDecimal.TEN).status(NavStatus.PUBLISHED).build();
        nav.setId(5L);
        when(navRepository.findById(5L)).thenReturn(Optional.of(nav));

        org.junit.jupiter.api.Assertions.assertThrows(
                com.fundmatrix.navaccounting.common.exception.BusinessException.class,
                () -> navService.publish(5L));
    }

    @Test
    void latestNavOrNullReturnsNullWhenNoneCaptured() {
        when(navRepository.findTopByOptionIdOrderByNavDateDesc(42L)).thenReturn(Optional.empty());
        assertThat(navService.latestNavOrNull(42L)).isNull();
    }

    @Test
    void requirePublishedNavThrowsBusinessExceptionWhenNonePublished() {
        when(navRepository.findTopByOptionIdAndStatusOrderByNavDateDesc(42L, NavStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                com.fundmatrix.navaccounting.common.exception.BusinessException.class,
                () -> navService.requirePublishedNav(42L));
    }
}
