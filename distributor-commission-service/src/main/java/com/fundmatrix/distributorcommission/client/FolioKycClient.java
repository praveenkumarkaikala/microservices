package com.fundmatrix.distributorcommission.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * Only the AUM/holdings read endpoints this service consumes from folio-transaction-service
 * (per FEIGN_CONTRACTS.md - HoldingInternalController). folio-transaction-service also exposes
 * folio/transaction/sip/swp endpoints and flag endpoints not needed here.
 */
@FeignClient(name = "folio-kyc-service", path = "/api")
public interface FolioKycClient {

    @GetMapping("/holdings/aum/distributor/{distributorId}")
    BigDecimal aumForDistributor(@PathVariable Long distributorId,
                                  @RequestParam(required = false) Long schemeId);

    @GetMapping("/holdings/folio-count/distributor/{distributorId}")
    Long folioCountForDistributor(@PathVariable Long distributorId);
}
