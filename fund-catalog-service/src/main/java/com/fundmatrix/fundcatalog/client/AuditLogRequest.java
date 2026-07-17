package com.fundmatrix.fundcatalog.client;

/** Mirrors audit-service's AuditLogRequest contract exactly (see FEIGN_CONTRACTS.md). */
public record AuditLogRequest(String action, String entityType, String recordId, String details,
                               Long actorId, String actorRole, String actorEmail) {
}
