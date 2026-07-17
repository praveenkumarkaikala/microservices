package com.fundmatrix.distributorcommission.service;

import com.fundmatrix.distributorcommission.client.AuditClient;
import com.fundmatrix.distributorcommission.client.AuditLogRequest;
import com.fundmatrix.distributorcommission.security.CurrentUserService;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around the audit-service Feign client. Keeps the same method signature the
 * monolith's local AuditService exposed so existing auditService.record(...) call sites in
 * DistributorService/CommissionService don't need to change.
 */
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
