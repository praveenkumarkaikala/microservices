package com.fundmatrix.compliance.service;

import com.fundmatrix.compliance.client.FolioKycClient;
import com.fundmatrix.compliance.client.TransactionClient;
import com.fundmatrix.compliance.common.exception.BusinessException;
import com.fundmatrix.compliance.domain.enums.FlagStatus;
import com.fundmatrix.compliance.dto.ComplianceKycStatusDto;
import com.fundmatrix.compliance.dto.ComplianceReportDto;
import com.fundmatrix.compliance.dto.KycRecordDto;
import com.fundmatrix.compliance.dto.TransactionFlagDto;
import com.fundmatrix.compliance.security.CurrentUserService;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock
    private FolioKycClient folioKycClient;
    @Mock
    private TransactionClient transactionClient;
    @Mock
    private CurrentUserService currentUser;
    @Mock
    private AuditService auditService;

    private ComplianceService complianceService;

    @BeforeEach
    void setUp() {
        complianceService = new ComplianceService(folioKycClient, transactionClient, currentUser, auditService);
    }

    /** Builds a TransactionFlagDto matching the current 11-field record, filling only the fields tests care about. */
    private TransactionFlagDto flag(Long id, BigDecimal amount, FlagStatus status) {
        return new TransactionFlagDto(id, id, "TXN-" + id, "FOL0000" + id, "Scheme " + id,
                amount, "reason " + id, status, null, Instant.now(), null);
    }

    @Test
    void kycStatus_countsRecordsByStatus() {
        List<KycRecordDto> records = List.of(
                new KycRecordDto(1L, "COMPLIANT", LocalDate.now()),
                new KycRecordDto(2L, "COMPLIANT", LocalDate.now()),
                new KycRecordDto(3L, "PENDING", LocalDate.now()),
                new KycRecordDto(4L, "NON_COMPLIANT", LocalDate.now()));
        when(folioKycClient.listKyc(null)).thenReturn(records);

        ComplianceKycStatusDto dto = complianceService.kycStatus();

        assertThat(dto.compliant()).isEqualTo(2L);
        assertThat(dto.pending()).isEqualTo(1L);
        assertThat(dto.nonCompliant()).isEqualTo(1L);
        assertThat(dto.expired()).isZero();
        assertThat(dto.total()).isEqualTo(4L);
    }

    @Test
    void kycStatus_wrapsFeignFailure_asBusinessException() {
        when(folioKycClient.listKyc(null)).thenThrow(feignInternalError());

        assertThatThrownBy(() -> complianceService.kycStatus())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void flags_delegatesToFeignClient_withStatusName() {
        TransactionFlagDto f = flag(1L, BigDecimal.TEN, FlagStatus.OPEN);
        when(transactionClient.flags("OPEN")).thenReturn(List.of(f));

        List<TransactionFlagDto> result = complianceService.flags(FlagStatus.OPEN);

        assertThat(result).containsExactly(f);
        verify(transactionClient).flags("OPEN");
    }

    @Test
    void flags_passesNullStatus_whenNoFilter() {
        when(transactionClient.flags(isNull())).thenReturn(List.of());

        complianceService.flags(null);

        verify(transactionClient).flags(isNull());
    }

    @Test
    void reviewFlag_delegatesToFeignClient_andAudits() {
        TransactionFlagDto updated = flag(1L, BigDecimal.TEN, FlagStatus.REVIEWED);
        when(transactionClient.reviewFlag(eq(1L), any())).thenReturn(updated);

        TransactionFlagDto result = complianceService.reviewFlag(1L, FlagStatus.REVIEWED, "looks fine");

        assertThat(result.status()).isEqualTo(FlagStatus.REVIEWED);
        ArgumentCaptor<Map<String, String>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(transactionClient).reviewFlag(eq(1L), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).containsEntry("status", "REVIEWED").containsEntry("note", "looks fine");
        verify(auditService).record(eq("FLAG_REVIEW"), eq("TransactionFlag"), eq(1L), anyString());
    }

    @Test
    void reviewFlag_wrapsFeignFailure_asBusinessException() {
        when(transactionClient.reviewFlag(eq(1L), any())).thenThrow(feignInternalError());

        assertThatThrownBy(() -> complianceService.reviewFlag(1L, FlagStatus.CLEARED, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void generateReport_aggregatesFlagCountsAndAmount() {
        lenient().when(currentUser.getEmail()).thenReturn("compliance@fundmatrix.com");
        when(folioKycClient.listKyc(null)).thenReturn(List.of());
        List<TransactionFlagDto> all = List.of(
                flag(1L, BigDecimal.valueOf(100), FlagStatus.OPEN),
                flag(2L, BigDecimal.valueOf(200), FlagStatus.CLEARED));
        when(transactionClient.flags(isNull())).thenReturn(all);

        ComplianceReportDto report = complianceService.generateReport();

        assertThat(report.flagsOpen()).isEqualTo(1);
        assertThat(report.flagsCleared()).isEqualTo(1);
        assertThat(report.flaggedTotalCount()).isEqualTo(2);
        assertThat(report.flaggedTotalAmount()).isEqualByComparingTo("300.00");
        verify(auditService).record(eq("COMPLIANCE_REPORT"), eq("Report"), isNull(), anyString());
    }

    
    
    
    
    
    
    
    
    
    
    
    private static FeignException feignInternalError() {
        Request request = Request.create(Request.HttpMethod.GET, "/x",
                Map.of(), null, new RequestTemplate());
        return new FeignException.InternalServerError("boom", request,
                "boom".getBytes(StandardCharsets.UTF_8), Map.of());
    }
}
