package com.fundmatrix.audit.service;

import com.fundmatrix.audit.domain.AuditLog;
import com.fundmatrix.audit.dto.AuditLogDto;
import com.fundmatrix.audit.dto.AuditLogRequest;
import com.fundmatrix.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository);
    }

    @Test
    void recordTruncatesDetailsAt500Characters() {
        String longDetails = "x".repeat(600);
        AuditLogRequest req = new AuditLogRequest(
                "CREATE", "Folio", "123", longDetails, 42L, "FUND_OPS", "ops@fundmatrix.com");

        auditService.record(req);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getDetails()).hasSize(500);
        assertThat(saved.getDetails()).isEqualTo("x".repeat(500));
        assertThat(saved.getUserId()).isEqualTo(42L);
        assertThat(saved.getUserName()).isEqualTo("ops@fundmatrix.com");
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getEntityType()).isEqualTo("Folio");
        assertThat(saved.getRecordId()).isEqualTo("123");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void recordWithNullDetailsDoesNotThrow() {
        AuditLogRequest req = new AuditLogRequest(
                "DELETE", "Transaction", "77", null, 5L, "ADMIN", "admin@fundmatrix.com");

        assertThatCodeDoesNotThrow(() -> auditService.record(req));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getDetails()).isNull();
    }

    @Test
    void listFiltersByEntityTypeAndRecordId() {
        AuditLog match = AuditLog.builder()
                .userId(1L).userName("a@fundmatrix.com").action("UPDATE")
                .entityType("Folio").recordId("10").details("d").timestamp(Instant.now())
                .build();
        when(auditLogRepository.findByEntityTypeAndRecordId("Folio", "10"))
                .thenReturn(List.of(match));

        List<AuditLogDto> result = auditService.list("Folio", "10");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).entityType()).isEqualTo("Folio");
        assertThat(result.get(0).recordId()).isEqualTo("10");
    }

    @Test
    void listWithoutFiltersReturnsAllOrderedByTimestampDesc() {
        AuditLog log1 = AuditLog.builder().action("A").timestamp(Instant.now()).build();
        when(auditLogRepository.findAllByOrderByTimestampDesc()).thenReturn(List.of(log1));

        List<AuditLogDto> result = auditService.list(null, null);

        assertThat(result).hasSize(1);
        verify(auditLogRepository).findAllByOrderByTimestampDesc();
    }

    private void assertThatCodeDoesNotThrow(Runnable runnable) {
        runnable.run();
    }
}
