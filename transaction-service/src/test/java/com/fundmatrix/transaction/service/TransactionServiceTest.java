package com.fundmatrix.transaction.service;

import com.fundmatrix.transaction.client.FolioKycClient;
import com.fundmatrix.transaction.client.FundCatalogClient;
import com.fundmatrix.transaction.client.NavAccountingClient;
import com.fundmatrix.transaction.common.exception.BusinessException;
import com.fundmatrix.transaction.domain.Transaction;
import com.fundmatrix.transaction.domain.enums.TransactionStatus;
import com.fundmatrix.transaction.domain.enums.TransactionType;
import com.fundmatrix.transaction.dto.CreditUnitsRequest;
import com.fundmatrix.transaction.dto.DebitUnitsRequest;
import com.fundmatrix.transaction.dto.FolioDto;
import com.fundmatrix.transaction.dto.HoldingDto;
import com.fundmatrix.transaction.dto.KycStatusDto;
import com.fundmatrix.transaction.dto.SchemeOptionDto;
import com.fundmatrix.transaction.dto.SubscriptionRequest;
import com.fundmatrix.transaction.dto.SwitchRequest;
import com.fundmatrix.transaction.dto.TransactionDto;
import com.fundmatrix.transaction.repository.AllotmentRepository;
import com.fundmatrix.transaction.repository.TransactionFlagRepository;
import com.fundmatrix.transaction.repository.TransactionRepository;
import com.fundmatrix.transaction.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adapted from the old folio-transaction-service's TransactionServiceTest. The main change is
 * that FolioService/HoldingService/ComplianceKycClient mocks are replaced by a single
 * FolioKycClient mock (plus a real FolioAccessService wired to it) since folio/holding/KYC
 * access is now a Feign call to folio-kyc-service instead of an in-process service call.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AllotmentRepository allotmentRepository;
    @Mock
    private TransactionFlagRepository flagRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditService auditService;
    @Mock
    private CurrentUserService currentUser;
    @Mock
    private FundCatalogClient fundCatalogClient;
    @Mock
    private NavAccountingClient navAccountingClient;
    @Mock
    private FolioKycClient folioKycClient;

    private TransactionService transactionService;

    private FolioDto activeFolio;
    private SchemeOptionDto option;

    @BeforeEach
    void setUp() {
        FolioAccessService folioAccessService = new FolioAccessService(folioKycClient, currentUser);
        transactionService = new TransactionService(transactionRepository, allotmentRepository,
                flagRepository, folioAccessService, notificationService,
                auditService, currentUser, new Mapper(), fundCatalogClient, navAccountingClient,
                folioKycClient, null, "15:00", "13:30");

        activeFolio = new FolioDto(1L, "FOL00001", 10L, null, "ACTIVE");

        option = new SchemeOptionDto(5L, 2L, "Growth Equity Fund", "GROWTH", "ACTIVE",
                "ACTIVE", "EQUITY", "15:00", new BigDecimal("1.0000"), new BigDecimal("1000.00"));

        lenient().when(folioKycClient.getFolio(1L)).thenReturn(activeFolio);
        lenient().when(folioKycClient.kycStatus(10L))
                .thenReturn(new KycStatusDto(10L, "COMPLIANT", true));
        lenient().when(fundCatalogClient.getOption(5L)).thenReturn(option);
        lenient().when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(100L);
            }
            return t;
        });
    }

    @Test
    void placeSubscription_happyPath_createsReceivedTransaction() {
        when(folioKycClient.getHoldingsByOption(5L)).thenReturn(List.of());

        SubscriptionRequest req = new SubscriptionRequest(1L, 5L, new BigDecimal("5000.00"));
        TransactionDto dto = transactionService.placeSubscription(req);

        assertThat(dto.status()).isEqualTo(TransactionStatus.RECEIVED);
        assertThat(dto.transactionType()).isEqualTo(TransactionType.SUBSCRIPTION);
        assertThat(dto.amount()).isEqualByComparingTo("5000.00");
        assertThat(dto.transactionRef()).isEqualTo("TXN00000100");
    }

    @Test
    void placeSubscription_belowMinInvestment_throwsBusinessException() {
        when(folioKycClient.getHoldingsByOption(5L)).thenReturn(List.of());

        SubscriptionRequest req = new SubscriptionRequest(1L, 5L, new BigDecimal("500.00"));

        assertThatThrownBy(() -> transactionService.placeSubscription(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Minimum investment");
    }

    @Test
    void placeSubscription_kycNonCompliant_blocked() {
        when(folioKycClient.kycStatus(10L)).thenReturn(new KycStatusDto(10L, "PENDING", false));

        SubscriptionRequest req = new SubscriptionRequest(1L, 5L, new BigDecimal("5000.00"));

        assertThatThrownBy(() -> transactionService.placeSubscription(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("KYC is not verified");
    }

    @Test
    void allot_subscription_computesUnitsFromNavAndCreditsHolding() {
        Transaction txn = Transaction.builder()
                .transactionRef("TXN00000200").folioId(1L).investorId(10L)
                .schemeId(2L).optionId(5L)
                .transactionType(TransactionType.SUBSCRIPTION)
                .amount(new BigDecimal("1000.00"))
                .status(TransactionStatus.RECEIVED)
                .build();
        txn.setId(200L);
        when(transactionRepository.findById(200L)).thenReturn(Optional.of(txn));
        when(navAccountingClient.requirePublishedNav(5L)).thenReturn(new BigDecimal("20.0000"));
        when(allotmentRepository.findByTransaction_Id(200L)).thenReturn(Optional.empty());

        TransactionDto dto = transactionService.allot(200L);

        // 1000 / 20 = 50 units
        assertThat(dto.units()).isEqualByComparingTo("50.0000");
        assertThat(dto.applicableNav()).isEqualByComparingTo("20.0000");
        assertThat(dto.status()).isEqualTo(TransactionStatus.ALLOTTED);
        verify(folioKycClient).creditUnits(new CreditUnitsRequest(1L, 2L, 5L,
                new BigDecimal("50.0000"), new BigDecimal("1000.00"), new BigDecimal("20.0000")));
    }

    @Test
    void switchUnits_debitsSourceAndCreditsTarget() {
        SchemeOptionDto toOption = new SchemeOptionDto(6L, 3L, "Debt Fund", "GROWTH", "ACTIVE",
                "ACTIVE", "DEBT", "15:00", BigDecimal.ZERO, new BigDecimal("1000.00"));
        when(fundCatalogClient.getOption(6L)).thenReturn(toOption);

        HoldingDto holding = new HoldingDto(9L, 1L, 2L, 5L,
                new BigDecimal("100.0000"), new BigDecimal("10.0000"), null, null, 10L);
        when(folioKycClient.getHoldingsByOption(5L)).thenReturn(List.of(holding));
        when(navAccountingClient.requirePublishedNav(5L)).thenReturn(new BigDecimal("20.0000"));
        when(navAccountingClient.requirePublishedNav(6L)).thenReturn(new BigDecimal("10.0000"));
        when(allotmentRepository.findByTransaction_Id(any())).thenReturn(Optional.empty());

        SwitchRequest req = new SwitchRequest(1L, 5L, 6L, new BigDecimal("40.0000"), false);
        List<TransactionDto> results = transactionService.switchUnits(req);

        assertThat(results).hasSize(2);
        TransactionDto out = results.get(0);
        TransactionDto in = results.get(1);
        assertThat(out.units()).isEqualByComparingTo("40.0000");
        assertThat(out.amount()).isEqualByComparingTo("800.00");   // 40 * 20
        // switch-in units = grossAmount(800) / toNav(10) = 80
        assertThat(in.units()).isEqualByComparingTo("80.0000");

        // Switch-out debit calls folio-kyc-service's dedicated /holdings/debit endpoint;
        // switch-in credits via /holdings/credit.
        ArgumentCaptor<DebitUnitsRequest> debitCaptor = ArgumentCaptor.forClass(DebitUnitsRequest.class);
        verify(folioKycClient).debitUnits(debitCaptor.capture());
        DebitUnitsRequest debitCall = debitCaptor.getValue();
        assertThat(debitCall.folioId()).isEqualTo(1L);
        assertThat(debitCall.optionId()).isEqualTo(5L);
        assertThat(debitCall.units()).isEqualByComparingTo("40.0000");
        assertThat(debitCall.navValue()).isEqualByComparingTo("20.0000");

        ArgumentCaptor<CreditUnitsRequest> creditCaptor = ArgumentCaptor.forClass(CreditUnitsRequest.class);
        verify(folioKycClient).creditUnits(creditCaptor.capture());
        assertThat(creditCaptor.getValue()).isEqualTo(new CreditUnitsRequest(1L, 3L, 6L,
                new BigDecimal("80.0000"), new BigDecimal("800.00"), new BigDecimal("10.0000")));
    }
}
