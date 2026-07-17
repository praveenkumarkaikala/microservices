package com.fundmatrix.audit.dto;

import java.time.Instant;

public record AuditLogDto(
        Long id,
        Long userId,
        String userName,
        String action,
        String entityType,
        String recordId,
        String details,
        Instant timestamp
) {
}
