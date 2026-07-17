package com.fundmatrix.transaction.controller;

import com.fundmatrix.transaction.dto.CreateSwpRequest;
import com.fundmatrix.transaction.dto.SwpMandateDto;
import com.fundmatrix.transaction.dto.UpdateSwpRequest;
import com.fundmatrix.transaction.dto.UpdateSwpStatusRequest;
import com.fundmatrix.transaction.service.SwpService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/swp-mandates")
@Tag(name = "SWP Mandates", description = "Systematic Withdrawal Plan mandates and instalments")
public class SwpController {

    private final SwpService swpService;

    public SwpController(SwpService swpService) {
        this.swpService = swpService;
    }

    @PostMapping
    public ResponseEntity<SwpMandateDto> create(@Valid @RequestBody CreateSwpRequest request) {
        return ResponseEntity.ok(swpService.create(request));
    }

    @GetMapping
    public List<SwpMandateDto> list() {
        return swpService.listForCurrentUser();
    }

    @GetMapping("/{swpId}")
    public SwpMandateDto get(@PathVariable Long swpId) {
        return swpService.get(swpId);
    }

    @PutMapping("/{swpId}")
    public SwpMandateDto update(@PathVariable Long swpId, @Valid @RequestBody UpdateSwpRequest request) {
        return swpService.update(swpId, request);
    }

    /** Pause / Resume (ACTIVE) / Cancel the mandate. */
    @PutMapping("/{swpId}/status")
    public SwpMandateDto updateStatus(@PathVariable Long swpId, @Valid @RequestBody UpdateSwpStatusRequest request) {
        return swpService.changeStatus(swpId, request.status());
    }

    /** Trigger one withdrawal instalment now (Fund Ops / Admin). */
    @PostMapping("/{swpId}/process")
    public SwpMandateDto process(@PathVariable Long swpId) {
        return swpService.process(swpId);
    }
}
