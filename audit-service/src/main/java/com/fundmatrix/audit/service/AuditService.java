package com.fundmatrix.audit.service;

import com.fundmatrix.audit.domain.AuditLog;
import com.fundmatrix.audit.dto.AuditLogDto;
import com.fundmatrix.audit.dto.AuditLogRequest;
import com.fundmatrix.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AuditService {

    private static final int MAX_DETAILS_LENGTH = 500;

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Persists an audit entry. Unlike the original monolith (which derived userId/userName
     * from the SecurityContext principal), this service trusts the actorId/actorRole/actorEmail
     * fields on the request body directly - they were resolved by the calling microservice's
     * own CurrentUserService from the original caller's identity. userName is populated from
     * actorEmail since the JWT carries no display name.
     */
    public void record(AuditLogRequest req) {
        String details = req.details();
        AuditLog log = AuditLog.builder()
                .userId(req.actorId())
                .userName(req.actorEmail() != null ? req.actorEmail() : "system")
                .action(req.action())
                .entityType(req.entityType())
                .recordId(req.recordId())
                .details(details != null && details.length() > MAX_DETAILS_LENGTH
                        ? details.substring(0, MAX_DETAILS_LENGTH) : details)
                .timestamp(Instant.now())
                .build();
        auditLogRepository.save(log);
    }

    public List<AuditLogDto> list(String entityType, String recordId) {
        List<AuditLog> logs;
        if (entityType != null && recordId != null) {
            logs = auditLogRepository.findByEntityTypeAndRecordId(entityType, recordId);
        } else {
            logs = auditLogRepository.findAllByOrderByTimestampDesc();
        }
        return logs.stream().map(this::toDto).toList();
    }

    public List<AuditLogDto> listAll() {
        return auditLogRepository.findAllByOrderByTimestampDesc().stream().map(this::toDto).toList();
    }

    private AuditLogDto toDto(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getUserId(),
                log.getUserName(),
                log.getAction(),
                log.getEntityType(),
                log.getRecordId(),
                log.getDetails(),
                log.getTimestamp());
    }
}
