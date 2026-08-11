package com.fundmatrix.compliance.service;

import com.fundmatrix.compliance.client.AuditClient;
import com.fundmatrix.compliance.client.AuditLogRequest;
import com.fundmatrix.compliance.security.CurrentUserService;
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
        String actorEmail = "system";
        try {
            actorId = currentUser.getId();
            actorRole = currentUser.getRole().name();
            actorEmail = currentUser.getEmail();
        } catch (Exception ignored) {
            // no authenticated user in context - treat as system
        }
        String truncatedDetails = details != null && details.length() > 500 ? details.substring(0, 500) : details;
        auditClient.record(new AuditLogRequest(action, entityType,
                recordId != null ? String.valueOf(recordId) : null, truncatedDetails,
                actorId, actorRole, actorEmail));
    }
}
