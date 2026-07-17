package com.fundmatrix.navaccounting.client;

public record AuditLogRequest(
        String action,
        String entityType,
        String recordId,
        String details,
        Long actorId,
        String actorRole,
        String actorEmail
) {
}
