package com.fundmatrix.compliance.client;

import com.fundmatrix.compliance.dto.KycRecordDto;
import com.fundmatrix.compliance.dto.KycStatusDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * compliance-service owns no KYC data at all - KycRecord lives in folio-kyc-service.
 * ComplianceService's kycStatus()/generateReport() delegate here instead of querying a
 * local KycRecordRepository. Only the subset of folio-kyc-service's KYC endpoints this
 * service actually needs is declared.
 */
@FeignClient(name = "folio-kyc-service", path = "/api")
public interface FolioKycClient {

    @GetMapping("/kyc/status/{investorId}")
    KycStatusDto kycStatus(@PathVariable Long investorId);

    @GetMapping("/kyc")
    List<KycRecordDto> listKyc(@RequestParam(required = false) String status);
}
