package com.fundmatrix.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "audit-service", path = "/api")
public interface AuditClient {

    record AuditLogRequest(String action, String entityType, String recordId, String details,
                           Long actorId, String actorRole, String actorEmail) {
    }

    @PostMapping("/audit/logs")
    void record(@RequestBody AuditLogRequest request);
}
