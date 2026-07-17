package com.fundmatrix.transaction.controller;

import com.fundmatrix.transaction.dto.AllotmentDto;
import com.fundmatrix.transaction.dto.AllotmentResultDto;
import com.fundmatrix.transaction.dto.BatchAllotRequest;
import com.fundmatrix.transaction.dto.RedemptionRequest;
import com.fundmatrix.transaction.dto.RejectTransactionRequest;
import com.fundmatrix.transaction.dto.SubscriptionRequest;
import com.fundmatrix.transaction.dto.SwitchRequest;
import com.fundmatrix.transaction.dto.TransactionDto;
import com.fundmatrix.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transactions", description = "Subscription, redemption, switch and the ops allotment workflow")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<TransactionDto> subscribe(@Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(transactionService.placeSubscription(request));
    }

    @PostMapping("/redemptions")
    public ResponseEntity<TransactionDto> redeem(@Valid @RequestBody RedemptionRequest request) {
        return ResponseEntity.ok(transactionService.placeRedemption(request));
    }

    @PostMapping("/switches")
    public ResponseEntity<List<TransactionDto>> switchUnits(@Valid @RequestBody SwitchRequest request) {
        return ResponseEntity.ok(transactionService.switchUnits(request));
    }

    @GetMapping
    public List<TransactionDto> list() {
        return transactionService.listForCurrentUser();
    }

    @GetMapping("/queue")
    public List<TransactionDto> queue() {
        return transactionService.queue();
    }

    @GetMapping("/flagged")
    public List<TransactionDto> flagged() {
        return transactionService.flaggedTransactions();
    }

    @GetMapping("/folio/{folioId}")
    public List<TransactionDto> byFolio(@PathVariable Long folioId) {
        return transactionService.listByFolio(folioId);
    }

    @GetMapping("/{id}/allotment")
    public AllotmentDto allotment(@PathVariable Long id) {
        return transactionService.getAllotment(id);
    }

    @PostMapping("/{id}/accept")
    public TransactionDto accept(@PathVariable Long id) {
        return transactionService.accept(id);
    }

    @PostMapping("/{id}/allot")
    public TransactionDto allot(@PathVariable Long id) {
        return transactionService.allot(id);
    }

    /** Batch-allot many transactions; returns a per-transaction success/failure result. */
    @PostMapping("/allot-batch")
    public List<AllotmentResultDto> allotBatch(@Valid @RequestBody BatchAllotRequest request) {
        return transactionService.allotBatch(request.transactionIds());
    }

    @PostMapping("/{id}/reject")
    public TransactionDto reject(@PathVariable Long id, @Valid @RequestBody RejectTransactionRequest request) {
        return transactionService.reject(id, request.reason());
    }
}
