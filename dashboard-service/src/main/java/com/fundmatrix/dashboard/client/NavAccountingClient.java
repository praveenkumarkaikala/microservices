package com.fundmatrix.dashboard.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

/**
 * Read-only consumer of nav-accounting-service (owns NavRecord). Used as a defensive
 * fallback to fill in a holding's latestNav when folio-transaction-service returns one
 * that hasn't been revalued yet (null) - mirrors the monolith's
 * HoldingService.latestNavOrNull() call inside DashboardService.investorDashboard().
 */
@FeignClient(name = "nav-accounting-service", path = "/api")
public interface NavAccountingClient {

    /** FEIGN_CONTRACTS.md internal contract. */
    @GetMapping("/nav/published/{optionId}")
    BigDecimal requirePublishedNav(@PathVariable Long optionId);

    /** FEIGN_CONTRACTS.md internal contract. */
    @GetMapping("/nav/latest/{optionId}")
    BigDecimal latestNavOrNull(@PathVariable Long optionId);
}
