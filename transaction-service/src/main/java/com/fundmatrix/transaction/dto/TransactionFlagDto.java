package com.fundmatrix.transaction.dto;

import com.fundmatrix.transaction.domain.enums.FlagStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Producer-side shape for GET /transactions/flags and PATCH /transactions/flags/{id}/review
 * (FEIGN_CONTRACTS.md's "transaction-service" section), consumed by compliance-service and
 * dashboard-service. Carries a few extra display fields (transactionRef/folioNumber/
 * schemeName/reviewNote/reviewedDate) beyond the minimal contract shape
 * (id, transactionId, reason, amount, status, createdDate) - consumers ignore unknown JSON
 * properties by default, so this superset stays wire-compatible with the documented contract.
 */
public record TransactionFlagDto(
        Long id,
        Long transactionId,
        String transactionRef,
        String folioNumber,
        String schemeName,
        BigDecimal amount,
        String reason,
        FlagStatus status,
        String reviewNote,
        Instant createdDate,
        Instant reviewedDate
) {
}
