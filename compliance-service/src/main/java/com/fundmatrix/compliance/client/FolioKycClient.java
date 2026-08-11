package com.fundmatrix.compliance.client;

import com.fundmatrix.compliance.dto.KycRecordDto;
import com.fundmatrix.compliance.dto.KycStatusDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "folio-kyc-service", path = "/api")
public interface FolioKycClient {

    @GetMapping("/kyc/status/{investorId}")
    KycStatusDto kycStatus(@PathVariable Long investorId);

    @GetMapping("/kyc")
    List<KycRecordDto> listKyc(@RequestParam(required = false) String status);
}
