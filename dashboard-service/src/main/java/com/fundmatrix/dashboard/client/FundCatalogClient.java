package com.fundmatrix.dashboard.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/** Read-only consumer of fund-catalog-service (owns FundScheme, SchemeOption). */
@FeignClient(name = "fund-catalog-service", path = "/api")
public interface FundCatalogClient {

    /** FEIGN_CONTRACTS.md internal contract. */
    @GetMapping("/schemes/{id}")
    SchemeDto getScheme(@PathVariable Long id);

    /** FEIGN_CONTRACTS.md internal contract. */
    @GetMapping("/schemes/options/{optionId}")
    SchemeOptionDto getOption(@PathVariable Long optionId);

    /** Existing monolith-derived admin listing route (SchemeController.list) - used for scheme counts. */
    @GetMapping("/schemes")
    List<SchemeDto> listSchemes();
}
