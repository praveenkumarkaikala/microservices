package com.fundmatrix.navaccounting.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Only the subset of folio-transaction-service's Feign surface that nav-accounting-service
 * actually calls (getHoldingsByOption/revalueOption for NAV publication, creditUnits for
 * dividend reinvestment, myFolios for the investor "my entitlements" view) - see
 * FEIGN_CONTRACTS.md's "folio-transaction-service" section for the full interface shared
 * across all consumers; other consumers (distributor-commission, compliance-kyc, dashboard)
 * declare their own client with the methods they need.
 */
@FeignClient(name = "folio-kyc-service", path = "/api")
public interface FolioKycClient {

    @GetMapping("/holdings/option/{optionId}")
    List<HoldingDto> getHoldingsByOption(@PathVariable Long optionId);

    @PostMapping("/holdings/option/{optionId}/revalue")
    Integer revalueOption(@PathVariable Long optionId, @RequestBody Map<String, BigDecimal> body);

    @PostMapping("/holdings/credit")
    HoldingDto creditUnits(@RequestBody CreditUnitsRequest request);

    @GetMapping("/folios")
    List<FolioDto> myFolios();
}
