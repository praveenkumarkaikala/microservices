package com.fundmatrix.transaction.service;

import com.fundmatrix.transaction.client.FolioKycClient;
import com.fundmatrix.transaction.client.FundCatalogClient;
import com.fundmatrix.transaction.common.exception.BusinessException;
import com.fundmatrix.transaction.domain.SipMandate;
import com.fundmatrix.transaction.domain.Transaction;
import com.fundmatrix.transaction.domain.enums.SipFrequency;
import com.fundmatrix.transaction.domain.enums.SipStatus;
import com.fundmatrix.transaction.dto.CreateSipRequest;
import com.fundmatrix.transaction.dto.FolioDto;
import com.fundmatrix.transaction.dto.SchemeDto;
import com.fundmatrix.transaction.dto.SchemeOptionDto;
import com.fundmatrix.transaction.dto.SipMandateDto;
import com.fundmatrix.transaction.repository.SipMandateRepository;
import com.fundmatrix.transaction.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Adapted from the old folio-transaction-service's SipServiceTest. FolioService is replaced by
 * a real FolioAccessService wired to a mocked FolioKycClient (Feign to folio-kyc-service now
 * that InvestorFolio no longer lives in this module).
 */
@ExtendWith(MockitoExtension.class)
class SipServiceTest {

    @Mock
    private SipMandateRepository sipRepository;
    @Mock
    private FundCatalogClient fundCatalogClient;
    @Mock
    private FolioKycClient folioKycClient;
    @Mock
    private TransactionService transactionService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditService auditService;
    @Mock
    private CurrentUserService currentUser;

    private SipService sipService;

    private FolioDto folio;
    private SchemeOptionDto option;

    @BeforeEach
    void setUp() {
        FolioAccessService folioAccessService = new FolioAccessService(folioKycClient, currentUser);
        sipService = new SipService(sipRepository, fundCatalogClient, folioAccessService, transactionService,
                notificationService, auditService, currentUser, new Mapper());

        folio = new FolioDto(1L, "FOL00001", 10L, null, "ACTIVE");

        option = new SchemeOptionDto(5L, 2L, "Growth Equity Fund", "GROWTH", "ACTIVE",
                "ACTIVE", "EQUITY", "15:00", new BigDecimal("1.0000"), new BigDecimal("1000.00"));

        lenient().when(folioKycClient.getFolio(1L)).thenReturn(folio);
        lenient().when(fundCatalogClient.getOption(5L)).thenReturn(option);
        lenient().when(sipRepository.save(any())).thenAnswer(inv -> {
            SipMandate m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId(9L);
            }
            return m;
        });
    }

    @Test
    void create_belowMinSipAmount_throws() {
        when(fundCatalogClient.getScheme(2L)).thenReturn(schemeWithMinSip(new BigDecimal("2000.00")));

        CreateSipRequest req = new CreateSipRequest(1L, 5L, new BigDecimal("500.00"),
                SipFrequency.MONTHLY, LocalDate.now(), null, null);

        assertThatThrownBy(() -> sipService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Minimum SIP amount");
    }

    @Test
    void create_happyPath_registersActiveMandate() {
        when(fundCatalogClient.getScheme(2L)).thenReturn(schemeWithMinSip(new BigDecimal("100.00")));

        CreateSipRequest req = new CreateSipRequest(1L, 5L, new BigDecimal("1000.00"),
                SipFrequency.MONTHLY, LocalDate.now(), null, 12);

        SipMandateDto dto = sipService.create(req);

        assertThat(dto.status()).isEqualTo(SipStatus.ACTIVE);
        assertThat(dto.mandateRef()).isEqualTo("SIP000009");
        assertThat(dto.amount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void runInstalment_advancesScheduleAndIncrementsExecutedCount() {
        SipMandate mandate = SipMandate.builder()
                .mandateRef("SIP000001").folioId(1L).investorId(10L).schemeId(2L).optionId(5L)
                .amount(new BigDecimal("1000.00")).frequency(SipFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 1, 1))
                .instalmentsExecuted(2)
                .nextInstalmentDate(LocalDate.of(2026, 3, 1))
                .status(SipStatus.ACTIVE)
                .build();
        mandate.setId(1L);
        when(sipRepository.findById(1L)).thenReturn(Optional.of(mandate));
        when(transactionService.placeAndAllotSipInstalment(mandate))
                .thenReturn(new Transaction());

        SipMandateDto dto = sipService.runInstalment(1L);

        assertThat(dto.instalmentsExecuted()).isEqualTo(3);
        assertThat(dto.nextInstalmentDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(dto.status()).isEqualTo(SipStatus.ACTIVE);
    }

    @Test
    void runInstalment_completesMandateWhenInstalmentCountReached() {
        SipMandate mandate = SipMandate.builder()
                .mandateRef("SIP000001").folioId(1L).investorId(10L).schemeId(2L).optionId(5L)
                .amount(new BigDecimal("1000.00")).frequency(SipFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 1, 1))
                .instalmentCount(3)
                .instalmentsExecuted(2)
                .nextInstalmentDate(LocalDate.of(2026, 3, 1))
                .status(SipStatus.ACTIVE)
                .build();
        mandate.setId(1L);
        when(sipRepository.findById(1L)).thenReturn(Optional.of(mandate));
        when(transactionService.placeAndAllotSipInstalment(mandate))
                .thenReturn(new Transaction());

        SipMandateDto dto = sipService.runInstalment(1L);

        assertThat(dto.instalmentsExecuted()).isEqualTo(3);
        assertThat(dto.status()).isEqualTo(SipStatus.COMPLETED);
    }

    private SchemeDto schemeWithMinSip(BigDecimal minSip) {
        return new SchemeDto(2L, "Growth Equity Fund", "GEF01", "EQUITY", "HIGH", "NIFTY50",
                1L, "Manager", new BigDecimal("1000.00"), "1%", BigDecimal.ZERO, 0,
                new BigDecimal("1.50"), minSip, new BigDecimal("500.00"), "15:00", "ACTIVE");
    }
}
