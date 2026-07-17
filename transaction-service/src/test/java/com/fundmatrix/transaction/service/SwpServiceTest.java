package com.fundmatrix.transaction.service;

import com.fundmatrix.transaction.client.FolioKycClient;
import com.fundmatrix.transaction.client.FundCatalogClient;
import com.fundmatrix.transaction.common.exception.BusinessException;
import com.fundmatrix.transaction.domain.SwpMandate;
import com.fundmatrix.transaction.domain.Transaction;
import com.fundmatrix.transaction.domain.enums.SipFrequency;
import com.fundmatrix.transaction.domain.enums.SipStatus;
import com.fundmatrix.transaction.dto.CreateSwpRequest;
import com.fundmatrix.transaction.dto.FolioDto;
import com.fundmatrix.transaction.dto.SchemeDto;
import com.fundmatrix.transaction.dto.SchemeOptionDto;
import com.fundmatrix.transaction.dto.SwpMandateDto;
import com.fundmatrix.transaction.dto.UpdateSwpRequest;
import com.fundmatrix.transaction.repository.SwpMandateRepository;
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
 * New test (no SwpServiceTest existed in the old folio-transaction-service), mirroring the
 * structure of SipServiceTest for parity - folio access goes through a real FolioAccessService
 * wired to a mocked FolioKycClient.
 */
@ExtendWith(MockitoExtension.class)
class SwpServiceTest {

    @Mock
    private SwpMandateRepository swpRepository;
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

    private SwpService swpService;

    private FolioDto folio;
    private SchemeOptionDto option;

    @BeforeEach
    void setUp() {
        FolioAccessService folioAccessService = new FolioAccessService(folioKycClient, currentUser);
        swpService = new SwpService(swpRepository, fundCatalogClient, folioAccessService, transactionService,
                notificationService, auditService, currentUser, new Mapper());

        folio = new FolioDto(1L, "FOL00001", 10L, null, "ACTIVE");

        option = new SchemeOptionDto(5L, 2L, "Growth Equity Fund", "GROWTH", "ACTIVE",
                "ACTIVE", "EQUITY", "15:00", new BigDecimal("1.0000"), new BigDecimal("1000.00"));

        lenient().when(folioKycClient.getFolio(1L)).thenReturn(folio);
        lenient().when(fundCatalogClient.getOption(5L)).thenReturn(option);
        lenient().when(swpRepository.save(any())).thenAnswer(inv -> {
            SwpMandate m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId(9L);
            }
            return m;
        });
    }

    @Test
    void create_belowMinSwpAmount_throws() {
        when(fundCatalogClient.getScheme(2L)).thenReturn(schemeWithMinSwp(new BigDecimal("2000.00")));

        CreateSwpRequest req = new CreateSwpRequest(1L, 5L, new BigDecimal("500.00"),
                SipFrequency.MONTHLY, LocalDate.now(), null, null);

        assertThatThrownBy(() -> swpService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Minimum SWP amount");
    }

    @Test
    void create_happyPath_registersActiveMandate() {
        when(fundCatalogClient.getScheme(2L)).thenReturn(schemeWithMinSwp(new BigDecimal("100.00")));

        CreateSwpRequest req = new CreateSwpRequest(1L, 5L, new BigDecimal("1000.00"),
                SipFrequency.MONTHLY, LocalDate.now(), null, 12);

        SwpMandateDto dto = swpService.create(req);

        assertThat(dto.status()).isEqualTo(SipStatus.ACTIVE);
        assertThat(dto.mandateRef()).isEqualTo("SWP000009");
        assertThat(dto.amount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void update_completedMandate_throws() {
        SwpMandate mandate = SwpMandate.builder()
                .mandateRef("SWP000001").folioId(1L).investorId(10L).schemeId(2L).optionId(5L)
                .amount(new BigDecimal("1000.00")).frequency(SipFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 1, 1))
                .instalmentsExecuted(3)
                .status(SipStatus.COMPLETED)
                .build();
        mandate.setId(1L);
        when(swpRepository.findById(1L)).thenReturn(Optional.of(mandate));

        UpdateSwpRequest req = new UpdateSwpRequest(new BigDecimal("2000.00"), null, null, null);

        assertThatThrownBy(() -> swpService.update(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot edit");
    }

    @Test
    void process_advancesScheduleAndIncrementsExecutedCount() {
        SwpMandate mandate = SwpMandate.builder()
                .mandateRef("SWP000001").folioId(1L).investorId(10L).schemeId(2L).optionId(5L)
                .amount(new BigDecimal("1000.00")).frequency(SipFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 1, 1))
                .instalmentsExecuted(2)
                .nextInstalmentDate(LocalDate.of(2026, 3, 1))
                .status(SipStatus.ACTIVE)
                .build();
        mandate.setId(1L);
        when(swpRepository.findById(1L)).thenReturn(Optional.of(mandate));
        when(transactionService.placeAndAllotSwpInstalment(mandate))
                .thenReturn(new Transaction());

        SwpMandateDto dto = swpService.process(1L);

        assertThat(dto.instalmentsExecuted()).isEqualTo(3);
        assertThat(dto.nextInstalmentDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(dto.status()).isEqualTo(SipStatus.ACTIVE);
    }

    private SchemeDto schemeWithMinSwp(BigDecimal minSwp) {
        return new SchemeDto(2L, "Growth Equity Fund", "GEF01", "EQUITY", "HIGH", "NIFTY50",
                1L, "Manager", new BigDecimal("1000.00"), "1%", BigDecimal.ZERO, 0,
                new BigDecimal("1.50"), new BigDecimal("500.00"), minSwp, "15:00", "ACTIVE");
    }
}
