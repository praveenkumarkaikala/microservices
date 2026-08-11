package com.fundmatrix.foliokyc.service;

import com.fundmatrix.foliokyc.client.AuditClient;
import com.fundmatrix.foliokyc.client.AuditClient.AuditLogRequest;
import com.fundmatrix.foliokyc.security.CurrentUserService;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditClient auditClient;
    private final CurrentUserService currentUser;

    public AuditService(AuditClient auditClient, CurrentUserService currentUser) {
        this.auditClient = auditClient;
        this.currentUser = currentUser;
    }

    public void record(String action, String entityType, Object recordId, String details) {
        Long actorId = null;
        String actorRole = null;
        String actorEmail = null;
        try {
            actorId = currentUser.getId();
            actorRole = currentUser.getRole() != null ? currentUser.getRole().name() : null;
            actorEmail = currentUser.getEmail();
        } catch (IllegalStateException ignored) {
            // no authenticated user in context (e.g. scheduled/internal call) - audit as system
        }
        String trimmed = details != null && details.length() > 500 ? details.substring(0, 500) : details;
        auditClient.record(new AuditLogRequest(action, entityType,
                recordId != null ? String.valueOf(recordId) : null, trimmed,
                actorId, actorRole, actorEmail));
    }
}
