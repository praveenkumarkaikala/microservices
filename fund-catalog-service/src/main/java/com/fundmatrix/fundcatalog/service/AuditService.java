package com.fundmatrix.fundcatalog.service;

import com.fundmatrix.fundcatalog.client.AuditClient;
import com.fundmatrix.fundcatalog.client.AuditLogRequest;
import com.fundmatrix.fundcatalog.security.AuthPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Thin Feign-backed replacement for the monolith's AuditService (which wrote directly to
 * AuditLogRepository). Same method signature as before so migrated call sites in SchemeService
 * don't change; the actor identity is read from the JWT-derived AuthPrincipal instead of a
 * DB-backed User entity, since this service doesn't own the user table.
 */
@Service
public class AuditService {

    private final AuditClient auditClient;

    public AuditService(AuditClient auditClient) {
        this.auditClient = auditClient;
    }

    public void record(String action, String entityType, Object recordId, String details) {
        Long actorId = null;
        String actorRole = null;
        String actorEmail = "system";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal principal) {
            actorId = principal.id();
            actorRole = principal.role() != null ? principal.role().name() : null;
            actorEmail = principal.email();
        }

        String truncatedDetails = details != null && details.length() > 500
                ? details.substring(0, 500) : details;

        auditClient.record(new AuditLogRequest(action, entityType,
                recordId != null ? String.valueOf(recordId) : null, truncatedDetails,
                actorId, actorRole, actorEmail));
    }
}
