package com.fundmatrix.transaction.client;

import com.fundmatrix.transaction.dto.CreditUnitsRequest;
import com.fundmatrix.transaction.dto.DebitUnitsRequest;
import com.fundmatrix.transaction.dto.FolioDto;
import com.fundmatrix.transaction.dto.HoldingDto;
import com.fundmatrix.transaction.dto.KycStatusDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Only the subset of folio-kyc-service's Feign surface transaction-service actually calls
 * (see FEIGN_CONTRACTS.md's "folio-kyc-service" section for the full interface shared across
 * all consumers): folio lookup for the access-control/active-folio checks that used to be
 * plain in-process FolioService calls, holdings-by-option for reading current unit balances,
 * creditUnits/debitUnits for allotment/instalment/redemption unit changes, and kycStatus for the pre-transaction KYC
 * gate (this used to be ComplianceKycClient pointed at compliance-kyc-service; KYC moved to
 * folio-kyc-service in this split so the client is renamed and repointed accordingly).
 * revalueOption() is NOT declared here - only nav-accounting-service calls that endpoint.
 */
@FeignClient(name = "folio-kyc-service", path = "/api")
public interface FolioKycClient {

    @GetMapping("/folios/{id}")
    FolioDto getFolio(@PathVariable Long id);

    @GetMapping("/holdings/option/{optionId}")
    List<HoldingDto> getHoldingsByOption(@PathVariable Long optionId);

    @PostMapping("/holdings/credit")
    HoldingDto creditUnits(@RequestBody CreditUnitsRequest request);

    @PostMapping("/holdings/debit")
    HoldingDto debitUnits(@RequestBody DebitUnitsRequest request);

    @GetMapping("/kyc/status/{investorId}")
    KycStatusDto kycStatus(@PathVariable Long investorId);
}
