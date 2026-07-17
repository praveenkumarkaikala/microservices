package com.fundmatrix.compliance.controller;

import com.fundmatrix.compliance.domain.enums.FlagStatus;
import com.fundmatrix.compliance.dto.ComplianceKycStatusDto;
import com.fundmatrix.compliance.dto.ComplianceReportDto;
import com.fundmatrix.compliance.dto.ReviewFlagRequest;
import com.fundmatrix.compliance.dto.TransactionFlagDto;
import com.fundmatrix.compliance.service.ComplianceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/compliance")
@Tag(name = "Compliance", description = "KYC monitoring, transaction-flag review and regulatory reports")
public class ComplianceController {

    private final ComplianceService complianceService;

    public ComplianceController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @GetMapping("/kyc-status")
    public ComplianceKycStatusDto kycStatus() {
        return complianceService.kycStatus();
    }

    @GetMapping("/transaction-flags")
    public List<TransactionFlagDto> flags(@RequestParam(required = false) FlagStatus status) {
        return complianceService.flags(status);
    }

    @PutMapping("/transaction-flags/{flagId}/review")
    public TransactionFlagDto review(@PathVariable Long flagId, @Valid @RequestBody ReviewFlagRequest request) {
        return complianceService.reviewFlag(flagId, request.status(), request.note());
    }

    @PostMapping("/reports/generate")
    public ComplianceReportDto generateReport() {
        return complianceService.generateReport();
    }
}
