package com.fundmatrix.audit.dto;

/**
 * Request body sent by every consuming service's AuditClient (Feign) via POST /audit/logs.
 * The calling service resolves actorId/actorRole/actorEmail from its own CurrentUserService
 * (the original caller's identity forwarded via the Authorization header) and passes them
 * explicitly here - this service trusts those fields directly rather than re-deriving them
 * from its own JWT parsing of the forwarded token.
 */
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
