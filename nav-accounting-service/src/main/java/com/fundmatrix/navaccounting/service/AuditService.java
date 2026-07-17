package com.fundmatrix.navaccounting.service;

import com.fundmatrix.navaccounting.client.AuditClient;
import com.fundmatrix.navaccounting.client.AuditLogRequest;
import com.fundmatrix.navaccounting.security.CurrentUserService;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around {@link AuditClient} that keeps the exact same call-site signature the
 * monolith's local AuditService had (`record(action, entityType, recordId, details)`), so
 * migrated NavService/AccrualService/DividendService code barely changes. actorId/actorRole/
 * actorEmail are resolved from CurrentUserService (i.e. from the JWT of the ORIGINAL caller,
 * forwarded to audit-service as-is - see FEIGN_CONTRACTS.md).
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditClient auditClient;
    private final CurrentUserService currentUser;

    public AuditService(AuditClient auditClient, CurrentUserService currentUser) {
        this.auditClient = auditClient;
        this.currentUser = currentUser;
    }

    public void record(String action, String entityType, Object recordId, String details) {
        try {
            auditClient.record(new AuditLogRequest(action, entityType,
                    recordId == null ? null : String.valueOf(recordId), details,
                    currentUser.getId(), currentUser.getRole().name(), currentUser.getEmail()));
        } catch (FeignException ex) {
            // Best-effort: an audit-service outage must not block the business transaction.
            log.warn("Failed to record audit log [{} {} {}]: {}", action, entityType, recordId, ex.getMessage());
        }
    }
}
