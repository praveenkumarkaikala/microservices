package com.fundmatrix.transaction.dto;

/** Per-transaction outcome of a batch allotment run (success or failure with reason). */
public record AllotmentResultDto(
        Long transactionId,
        String transactionRef,
        boolean success,
        String status,
        String message
) {
}
