package com.fundmatrix.compliance.dto;

import com.fundmatrix.compliance.domain.enums.FlagStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Plain response shape for transaction-service's transaction-flags endpoints
 * (GET /transactions/flags, PATCH /transactions/flags/{id}/review). TransactionFlag itself
 * is owned by transaction-service - this is just the wire shape for the Feign call,
 * not a JPA entity.
 */
public record TransactionFlagDto(
        Long id,
        Long transactionId,
        String reason,
        BigDecimal amount,
        FlagStatus status,
        Instant createdDate
) {
}
