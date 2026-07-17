package com.fundmatrix.transaction.controller;

import com.fundmatrix.transaction.domain.enums.SipStatus;
import com.fundmatrix.transaction.dto.CreateSipRequest;
import com.fundmatrix.transaction.dto.SipMandateDto;
import com.fundmatrix.transaction.service.SipService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sips")
@Tag(name = "SIP Mandates", description = "Systematic Investment Plan mandates and instalments")
public class SipController {

    private final SipService sipService;

    public SipController(SipService sipService) {
        this.sipService = sipService;
    }

    @GetMapping
    public List<SipMandateDto> list() {
        return sipService.listForCurrentUser();
    }

    @GetMapping("/due")
    public List<SipMandateDto> due() {
        return sipService.dueMandates();
    }

    @PostMapping
    public ResponseEntity<SipMandateDto> create(@Valid @RequestBody CreateSipRequest request) {
        return ResponseEntity.ok(sipService.create(request));
    }

    @PostMapping("/{id}/run")
    public SipMandateDto run(@PathVariable Long id) {
        return sipService.runInstalment(id);
    }

    @PostMapping("/{id}/pause")
    public SipMandateDto pause(@PathVariable Long id) {
        return sipService.changeStatus(id, SipStatus.PAUSED);
    }

    @PostMapping("/{id}/resume")
    public SipMandateDto resume(@PathVariable Long id) {
        return sipService.changeStatus(id, SipStatus.ACTIVE);
    }

    @PostMapping("/{id}/cancel")
    public SipMandateDto cancel(@PathVariable Long id) {
        return sipService.changeStatus(id, SipStatus.CANCELLED);
    }
}
