package com.fundmatrix.distributorcommission.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "fund-catalog-service", path = "/api")
public interface FundCatalogClient {

    @GetMapping("/schemes/{id}")
    SchemeDto getScheme(@PathVariable Long id);
}
