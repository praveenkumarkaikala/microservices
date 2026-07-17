package com.fundmatrix.compliance.client;

import com.fundmatrix.compliance.dto.TransactionFlagDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * compliance-service does not own TransactionFlag - it lives in transaction-service
 * (created there when a large transaction is auto-flagged). ComplianceService's flags()/
 * reviewFlag() delegate here instead of querying a local repository.
 */
@FeignClient(name = "transaction-service", path = "/api")
public interface TransactionClient {

    @GetMapping("/transactions/flags")
    List<TransactionFlagDto> flags(@RequestParam(required = false) String status);

    @PatchMapping("/transactions/flags/{id}/review")
    TransactionFlagDto reviewFlag(@PathVariable Long id, @RequestBody Map<String, String> body);
}
