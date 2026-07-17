package com.fundmatrix.foliokyc.controller;

import com.fundmatrix.foliokyc.domain.enums.KycStatus;
import com.fundmatrix.foliokyc.dto.KycRecordDto;
import com.fundmatrix.foliokyc.dto.KycStatusDto;
import com.fundmatrix.foliokyc.dto.SubmitKycRequest;
import com.fundmatrix.foliokyc.dto.UpdateKycStatusRequest;
import com.fundmatrix.foliokyc.service.KycService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/kyc")
@Tag(name = "KYC", description = "KYC capture and verification")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @GetMapping
    public List<KycRecordDto> list(@RequestParam(required = false) KycStatus status) {
        return kycService.list(status);
    }

    @GetMapping("/investor/{investorId}")
    public List<KycRecordDto> forInvestor(@PathVariable Long investorId) {
        return kycService.listForInvestor(investorId);
    }

    @GetMapping("/mine")
    public List<KycRecordDto> mine() {
        return kycService.mine();
    }

    /** Investor self-submits (uploads) their own KYC details for verification. */
    @PostMapping
    public ResponseEntity<KycRecordDto> submit(@Valid @RequestBody SubmitKycRequest request) {
        return ResponseEntity.ok(kycService.submit(request));
    }

    /** Fund Ops / Compliance verify an investor-submitted KYC (approve / reject). */
    @PatchMapping("/{id}/status")
    public KycRecordDto updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateKycStatusRequest request) {
        return kycService.updateStatus(id, request.kycStatus());
    }

    /**
     * Internal endpoint consumed by transaction-service, nav-accounting-service,
     * distributor-commission-service, compliance-service and dashboard-service as the KYC
     * gate/lookup. Not role-restricted in SecurityConfig - any authenticated caller (the
     * forwarded original caller's token) may query it.
     */
    @GetMapping("/status/{investorId}")
    public KycStatusDto status(@PathVariable Long investorId) {
        return kycService.kycStatusFor(investorId);
    }
}
