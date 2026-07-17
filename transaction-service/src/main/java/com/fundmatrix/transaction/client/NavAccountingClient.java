package com.fundmatrix.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "nav-accounting-service", path = "/api")
public interface NavAccountingClient {

    @GetMapping("/nav/published/{optionId}")
    BigDecimal requirePublishedNav(@PathVariable Long optionId);

    @GetMapping("/nav/latest/{optionId}")
    BigDecimal latestNavOrNull(@PathVariable Long optionId);
}
