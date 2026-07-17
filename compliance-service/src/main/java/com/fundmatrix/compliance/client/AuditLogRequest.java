package com.fundmatrix.compliance.client;

public record AuditLogRequest(String action, String entityType, String recordId, String details,
                               Long actorId, String actorRole, String actorEmail) {
}
