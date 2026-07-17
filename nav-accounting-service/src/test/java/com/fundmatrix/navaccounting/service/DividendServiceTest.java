package com.fundmatrix.navaccounting.service;

import com.fundmatrix.navaccounting.client.CreditUnitsRequest;
import com.fundmatrix.navaccounting.client.FolioKycClient;
import com.fundmatrix.navaccounting.client.FundCatalogClient;
import com.fundmatrix.navaccounting.client.HoldingDto;
import com.fundmatrix.navaccounting.domain.DividendDeclaration;
import com.fundmatrix.navaccounting.domain.InvestorDividendEntitlement;
import com.fundmatrix.navaccounting.domain.enums.DividendStatus;
import com.fundmatrix.navaccounting.domain.enums.EntitlementStatus;
import com.fundmatrix.navaccounting.domain.enums.OptionType;
import com.fundmatrix.navaccounting.domain.enums.PayoutMode;
import com.fundmatrix.navaccounting.dto.EntitlementDto;
import com.fundmatrix.navaccounting.repository.DividendDeclarationRepository;
import com.fundmatrix.navaccounting.repository.InvestorDividendEntitlementRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DividendServiceTest {

    private static final BigDecimal TDS_RATE = new BigDecimal("10.0000"); // 10%

    @Mock
    private DividendDeclarationRepository declarationRepository;
    @Mock
    private InvestorDividendEntitlementRepository entitlementRepository;
    @Mock
    private FundCatalogClient fundCatalogClient;
    @Mock
    private FolioKycClient folioTransactionClient;
    @Mock
    private NavService navService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditService auditService;
    @Mock
    private CurrentUserService currentUser;

    private DividendService dividendService;

    @BeforeEach
    void setUp() {
        dividendService = new DividendService(declarationRepository, entitlementRepository, fundCatalogClient,
                folioTransactionClient, navService, notificationService, auditService, currentUser, new Mapper(),
                TDS_RATE);
    }

    private DividendDeclaration declaration(Long id, OptionType optionType, DividendStatus status) {
        DividendDeclaration d = DividendDeclaration.builder()
                .schemeId(10L).schemeName("Blue Chip Fund")
                .optionId(20L).optionType(optionType)
                .recordDate(LocalDate.of(2026, 7, 1))
                .dividendPerUnit(new BigDecimal("2.0000"))
                .totalDistributionAmount(BigDecimal.ZERO)
                .declaredById(1L)
                .status(status)
                .build();
        d.setId(id);
        return d;
    }

    @Test
    void computeEntitlementsTdsMathIsCorrect() {
        DividendDeclaration declaration = declaration(100L, OptionType.DIVIDEND_PAYOUT, DividendStatus.DECLARED);
        when(declarationRepository.findById(100L)).thenReturn(Optional.of(declaration));

        HoldingDto holding = new HoldingDto(1L, 500L, 10L, 20L, new BigDecimal("1000.0000"),
                new BigDecimal("95.0000"), null, null, 900L);
        when(folioTransactionClient.getHoldingsByOption(20L)).thenReturn(List.of(holding));
        when(declarationRepository.save(any(DividendDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));

        // After compute, getEntitlements() re-reads from the repo - simulate persistence.
        InvestorDividendEntitlement saved = InvestorDividendEntitlement.builder()
                .declaration(declaration).folioId(500L).investorId(900L)
                .unitsOnRecordDate(new BigDecimal("1000.0000"))
                .grossDividend(new BigDecimal("2000.00"))
                .taxDeducted(new BigDecimal("200.00"))
                .netDividend(new BigDecimal("1800.00"))
                .payoutMode(PayoutMode.BANK_CREDIT)
                .status(EntitlementStatus.COMPUTED)
                .build();
        saved.setId(1L);
        when(entitlementRepository.findByDeclarationId(100L)).thenReturn(List.of(), List.of(saved));

        List<EntitlementDto> result = dividendService.computeEntitlements(100L);

        ArgumentCaptor<InvestorDividendEntitlement> captor =
                ArgumentCaptor.forClass(InvestorDividendEntitlement.class);
        verify(entitlementRepository).save(captor.capture());
        InvestorDividendEntitlement persisted = captor.getValue();

        // units 1000 * dpu 2.00 = gross 2000.00; tax = 10% of 2000 = 200.00; net = 1800.00
        assertThat(persisted.getGrossDividend()).isEqualByComparingTo("2000.00");
        assertThat(persisted.getTaxDeducted()).isEqualByComparingTo("200.00");
        assertThat(persisted.getNetDividend()).isEqualByComparingTo("1800.00");
        assertThat(persisted.getPayoutMode()).isEqualTo(PayoutMode.BANK_CREDIT);
        assertThat(persisted.getInvestorId()).isEqualTo(900L);

        assertThat(declaration.getTotalDistributionAmount()).isEqualByComparingTo("2000.00");
        assertThat(result).hasSize(1);
    }

    @Test
    void processReinvestsWhenPayoutModeIsReinvestmentAndNavIsAvailable() {
        DividendDeclaration declaration = declaration(200L, OptionType.DIVIDEND_REINVESTMENT, DividendStatus.APPROVED);
        when(declarationRepository.findById(200L)).thenReturn(Optional.of(declaration));

        InvestorDividendEntitlement reinvestEntitlement = InvestorDividendEntitlement.builder()
                .declaration(declaration).folioId(500L).investorId(900L)
                .unitsOnRecordDate(new BigDecimal("1000.0000"))
                .grossDividend(new BigDecimal("2000.00")).taxDeducted(new BigDecimal("200.00"))
                .netDividend(new BigDecimal("1800.00"))
                .payoutMode(PayoutMode.REINVESTMENT).status(EntitlementStatus.COMPUTED)
                .build();
        reinvestEntitlement.setId(1L);

        when(entitlementRepository.findByDeclarationId(200L)).thenReturn(List.of(reinvestEntitlement));
        when(navService.latestNavOrNull(20L)).thenReturn(new BigDecimal("18.0000"));
        when(declarationRepository.save(any(DividendDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));

        dividendService.process(200L);

        // net 1800.00 / nav 18.0000 = 100.0000 units credited back.
        ArgumentCaptor<CreditUnitsRequest> captor = ArgumentCaptor.forClass(CreditUnitsRequest.class);
        verify(folioTransactionClient).creditUnits(captor.capture());
        assertThat(captor.getValue().units()).isEqualByComparingTo("100.0000");
        assertThat(captor.getValue().folioId()).isEqualTo(500L);

        assertThat(reinvestEntitlement.getStatus()).isEqualTo(EntitlementStatus.REINVESTED);
        verify(notificationService).notify(eq(900L), any(), any());
        assertThat(declaration.getStatus()).isEqualTo(DividendStatus.PROCESSED);
    }

    @Test
    void processCreditsBankAccountWhenPayoutModeIsBankCredit() {
        DividendDeclaration declaration = declaration(300L, OptionType.DIVIDEND_PAYOUT, DividendStatus.APPROVED);
        when(declarationRepository.findById(300L)).thenReturn(Optional.of(declaration));

        InvestorDividendEntitlement bankEntitlement = InvestorDividendEntitlement.builder()
                .declaration(declaration).folioId(501L).investorId(901L)
                .unitsOnRecordDate(new BigDecimal("500.0000"))
                .grossDividend(new BigDecimal("1000.00")).taxDeducted(new BigDecimal("100.00"))
                .netDividend(new BigDecimal("900.00"))
                .payoutMode(PayoutMode.BANK_CREDIT).status(EntitlementStatus.COMPUTED)
                .build();
        bankEntitlement.setId(2L);

        when(entitlementRepository.findByDeclarationId(300L)).thenReturn(List.of(bankEntitlement));
        when(declarationRepository.save(any(DividendDeclaration.class))).thenAnswer(inv -> inv.getArgument(0));

        dividendService.process(300L);

        verify(folioTransactionClient, never()).creditUnits(any());
        assertThat(bankEntitlement.getStatus()).isEqualTo(EntitlementStatus.DISBURSED);
        verify(notificationService, times(1)).notify(eq(901L), any(), any());
        assertThat(declaration.getStatus()).isEqualTo(DividendStatus.PROCESSED);
    }

    @Test
    void processThrowsWhenDeclarationIsNotApproved() {
        DividendDeclaration declaration = declaration(400L, OptionType.DIVIDEND_PAYOUT, DividendStatus.DECLARED);
        when(declarationRepository.findById(400L)).thenReturn(Optional.of(declaration));

        org.junit.jupiter.api.Assertions.assertThrows(
                com.fundmatrix.navaccounting.common.exception.BusinessException.class,
                () -> dividendService.process(400L));
    }
}
