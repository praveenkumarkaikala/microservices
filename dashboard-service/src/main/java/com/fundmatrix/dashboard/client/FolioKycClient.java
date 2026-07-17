package com.fundmatrix.dashboard.client;

import com.fundmatrix.dashboard.dto.FolioHoldingDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * Read-only consumer of folio-kyc-service (owns InvestorFolio, FolioHolding, KycRecord - Folio,
 * Holding and KYC were merged into one service; Transaction/SIP/SWP live separately in
 * transaction-service, see {@link TransactionClient}).
 */
@FeignClient(name = "folio-kyc-service", path = "/api")
public interface FolioKycClient {

    /** FEIGN_CONTRACTS.md internal contract - used for the distributor dashboard AUM figure. */
    @GetMapping("/holdings/aum/distributor/{distributorId}")
    BigDecimal aumForDistributor(@PathVariable Long distributorId,
                                  @RequestParam(required = false) Long schemeId);

    /** FEIGN_CONTRACTS.md internal contract - used for the distributor dashboard folio count. */
    @GetMapping("/holdings/folio-count/distributor/{distributorId}")
    Long folioCountForDistributor(@PathVariable Long distributorId);

    /**
     * Existing monolith-derived route (FolioController.list -> FolioService.listForCurrentUser):
     * scoped by role - INVESTOR gets only their own folios, ADMIN/other staff roles get all
     * folios. Authorization forwarded via FeignConfig, so calling this with the dashboard
     * caller's own JWT reproduces the exact same scoping the monolith had in-process.
     */
    @GetMapping("/folios")
    List<FolioDto> listFolios();

    /** Existing monolith-derived route (FolioController.holdings). */
    @GetMapping("/folios/{id}/holdings")
    List<FolioHoldingDto> folioHoldings(@PathVariable("id") Long folioId);

    /** FEIGN_CONTRACTS.md internal contract - used for the compliance summary counts. */
    @GetMapping("/kyc/status/{investorId}")
    KycStatusDto kycStatus(@PathVariable Long investorId);

    /** Existing monolith-derived route (KycController.list) - used for the compliance summary counts. */
    @GetMapping("/kyc")
    List<KycRecordDto> listByStatus(@RequestParam(required = false) String status);
}
