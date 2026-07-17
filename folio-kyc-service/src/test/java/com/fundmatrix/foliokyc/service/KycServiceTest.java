package com.fundmatrix.foliokyc.service;

import com.fundmatrix.foliokyc.client.NotificationClient;
import com.fundmatrix.foliokyc.client.NotificationClient.NotificationRequest;
import com.fundmatrix.foliokyc.common.exception.BusinessException;
import com.fundmatrix.foliokyc.common.exception.ResourceNotFoundException;
import com.fundmatrix.foliokyc.domain.KycRecord;
import com.fundmatrix.foliokyc.domain.enums.KycStatus;
import com.fundmatrix.foliokyc.domain.enums.KycType;
import com.fundmatrix.foliokyc.domain.enums.Role;
import com.fundmatrix.foliokyc.dto.KycRecordDto;
import com.fundmatrix.foliokyc.dto.KycStatusDto;
import com.fundmatrix.foliokyc.dto.SubmitKycRequest;
import com.fundmatrix.foliokyc.repository.KycRecordRepository;
import com.fundmatrix.foliokyc.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    @Mock
    private KycRecordRepository kycRepository;
    @Mock
    private NotificationClient notificationClient;
    @Mock
    private AuditService auditService;
    @Mock
    private CurrentUserService currentUser;

    private KycService kycService;

    @BeforeEach
    void setUp() {
        kycService = new KycService(kycRepository, notificationClient, auditService, currentUser, new Mapper());
    }

    @Test
    void submit_createsPendingRecord_forInvestor() {
        when(currentUser.getRole()).thenReturn(Role.INVESTOR);
        when(currentUser.getId()).thenReturn(42L);
        when(kycRepository.save(any(KycRecord.class))).thenAnswer(inv -> {
            KycRecord r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        SubmitKycRequest req = new SubmitKycRequest(KycType.FULL, "PAN", "ABCDE1234F");
        KycRecordDto dto = kycService.submit(req);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.investorId()).isEqualTo(42L);
        assertThat(dto.kycStatus()).isEqualTo(KycStatus.PENDING);

        verify(auditService).record("KYC_SUBMIT", "KycRecord", 1L, "KYC submitted by investor 42");
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient).notify(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(42L);
        assertThat(captor.getValue().category()).isEqualTo("KYC");
    }

    @Test
    void submit_rejectsNonInvestor() {
        when(currentUser.getRole()).thenReturn(Role.FUND_OPS);

        SubmitKycRequest req = new SubmitKycRequest(KycType.FULL, "PAN", "ABCDE1234F");

        assertThatThrownBy(() -> kycService.submit(req)).isInstanceOf(BusinessException.class);
    }

    @Test
    void updateStatus_setsVerifiedDate_whenCompliant() {
        KycRecord record = KycRecord.builder()
                .investorId(7L).kycType(KycType.FULL).documentType("PAN").documentRef("X")
                .kycStatus(KycStatus.PENDING).build();
        record.setId(5L);
        when(kycRepository.findById(5L)).thenReturn(Optional.of(record));
        when(kycRepository.save(any(KycRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        KycRecordDto dto = kycService.updateStatus(5L, KycStatus.COMPLIANT);

        assertThat(dto.kycStatus()).isEqualTo(KycStatus.COMPLIANT);
        assertThat(dto.verifiedDate()).isNotNull();
        verify(auditService).record("KYC_STATUS", "KycRecord", 5L, "KYC status set to COMPLIANT");
        verify(notificationClient).notify(any(NotificationRequest.class));
    }

    @Test
    void updateStatus_throwsWhenMissing() {
        when(kycRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kycService.updateStatus(99L, KycStatus.COMPLIANT))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void kycStatusFor_compliant_whenExistsCompliantRecord() {
        when(kycRepository.existsByInvestorIdAndKycStatus(7L, KycStatus.COMPLIANT)).thenReturn(true);
        when(kycRepository.findByInvestorId(7L)).thenReturn(List.of());

        KycStatusDto dto = kycService.kycStatusFor(7L);

        assertThat(dto.compliant()).isTrue();
        assertThat(dto.kycStatus()).isEqualTo("COMPLIANT");
        assertThat(dto.investorId()).isEqualTo(7L);
    }

    @Test
    void kycStatusFor_nonCompliant_whenNoCompliantRecord() {
        KycRecord record = KycRecord.builder()
                .investorId(8L).kycType(KycType.FULL).documentType("PAN").documentRef("X")
                .kycStatus(KycStatus.NON_COMPLIANT).build();
        record.setId(3L);
        when(kycRepository.existsByInvestorIdAndKycStatus(8L, KycStatus.COMPLIANT)).thenReturn(false);
        when(kycRepository.findByInvestorId(8L)).thenReturn(List.of(record));

        KycStatusDto dto = kycService.kycStatusFor(8L);

        assertThat(dto.compliant()).isFalse();
        assertThat(dto.kycStatus()).isEqualTo("NON_COMPLIANT");
    }
}
