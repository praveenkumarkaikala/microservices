package com.fundmatrix.navaccounting.controller;

import com.fundmatrix.navaccounting.domain.enums.DividendStatus;
import com.fundmatrix.navaccounting.dto.CreateDividendRequest;
import com.fundmatrix.navaccounting.dto.DividendDeclarationDto;
import com.fundmatrix.navaccounting.dto.EntitlementDto;
import com.fundmatrix.navaccounting.service.DividendService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dividends")
@Tag(name = "Dividends", description = "Dividend declaration, entitlement computation and payouts")
public class DividendController {

    private final DividendService dividendService;

    public DividendController(DividendService dividendService) {
        this.dividendService = dividendService;
    }

    @GetMapping
    public List<DividendDeclarationDto> list(@RequestParam(required = false) DividendStatus status) {
        return dividendService.list(status);
    }

    @GetMapping("/{id}/entitlements")
    public List<EntitlementDto> entitlements(@PathVariable Long id) {
        return dividendService.getEntitlements(id);
    }

    @GetMapping("/entitlements/mine")
    public List<EntitlementDto> myEntitlements() {
        return dividendService.myEntitlements();
    }

    @PostMapping
    public ResponseEntity<DividendDeclarationDto> declare(@Valid @RequestBody CreateDividendRequest request) {
        return ResponseEntity.ok(dividendService.declare(request));
    }

    @PostMapping("/{id}/compute")
    public List<EntitlementDto> compute(@PathVariable Long id) {
        return dividendService.computeEntitlements(id);
    }

    @PostMapping("/{id}/approve")
    public DividendDeclarationDto approve(@PathVariable Long id) {
        return dividendService.approve(id);
    }

    @PostMapping("/{id}/process")
    public DividendDeclarationDto process(@PathVariable Long id) {
        return dividendService.process(id);
    }

    @PostMapping("/{id}/cancel")
    public DividendDeclarationDto cancel(@PathVariable Long id) {
        return dividendService.cancel(id);
    }
}
