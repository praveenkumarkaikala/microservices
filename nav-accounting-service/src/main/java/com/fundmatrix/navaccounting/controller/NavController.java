package com.fundmatrix.navaccounting.controller;

import com.fundmatrix.navaccounting.dto.AumSummaryDto;
import com.fundmatrix.navaccounting.dto.NavRecordDto;
import com.fundmatrix.navaccounting.dto.SaveNavRequest;
import com.fundmatrix.navaccounting.service.NavService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/nav")
@Tag(name = "NAV & Fund Accounting", description = "NAV capture, publication and AUM reporting")
public class NavController {

    private final NavService navService;

    public NavController(NavService navService) {
        this.navService = navService;
    }

    @GetMapping("/scheme/{schemeId}")
    public List<NavRecordDto> byScheme(@PathVariable Long schemeId) {
        return navService.listByScheme(schemeId);
    }

    @GetMapping("/option/{optionId}")
    public List<NavRecordDto> byOption(@PathVariable Long optionId) {
        return navService.listByOption(optionId);
    }

    @GetMapping("/aum-summary")
    public List<AumSummaryDto> aumSummary() {
        return navService.aumSummary();
    }

    @PostMapping
    public ResponseEntity<NavRecordDto> save(@Valid @RequestBody SaveNavRequest request) {
        return ResponseEntity.ok(navService.saveNavInput(request));
    }

    @PostMapping("/{id}/publish")
    public NavRecordDto publish(@PathVariable Long id) {
        return navService.publish(id);
    }

    /**
     * Internal, service-to-service endpoint (FEIGN_CONTRACTS.md). Consumed by
     * folio-transaction-service's NavAccountingClient.requirePublishedNav(optionId) wherever the
     * monolith used to call HoldingService.requirePublishedNav(optionId) directly.
     */
    @GetMapping("/published/{optionId}")
    public BigDecimal published(@PathVariable Long optionId) {
        return navService.requirePublishedNav(optionId);
    }

    /**
     * Internal, service-to-service endpoint (FEIGN_CONTRACTS.md). Consumed by
     * folio-transaction-service's NavAccountingClient.latestNavOrNull(optionId).
     */
    @GetMapping("/latest/{optionId}")
    public BigDecimal latest(@PathVariable Long optionId) {
        return navService.latestNavOrNull(optionId);
    }
}
