package com.fundmatrix.compliance.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "audit-service", path = "/api")
public interface AuditClient {

    @PostMapping("/audit/logs")
    void record(@RequestBody AuditLogRequest request);
}
