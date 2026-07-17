package com.fundmatrix.dashboard.client;

import com.fundmatrix.dashboard.dto.TransactionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Read-only consumer of transaction-service (owns Transaction, Allotment, TransactionFlag,
 * SipMandate, SwpMandate - split out of the old folio-transaction-service, separate from Folio/
 * Holding/KYC which live in folio-kyc-service, see {@link FolioKycClient}).
 */
@FeignClient(name = "transaction-service", path = "/api")
public interface TransactionClient {

    /**
     * Existing monolith-derived route (SipController.list -> SipService.listForCurrentUser):
     * INVESTOR gets only their own SIP mandates, other roles get all.
     */
    @GetMapping("/sips")
    List<SipMandateDto> sips();

    /** Existing monolith-derived route (TransactionController.queue) - global pending queue, not user-scoped. */
    @GetMapping("/transactions/queue")
    List<TransactionDto> transactionQueue();

    /** Existing monolith-derived route (TransactionController.flagged) - global large-amount flag list. */
    @GetMapping("/transactions/flagged")
    List<TransactionDto> flaggedTransactions();
}
