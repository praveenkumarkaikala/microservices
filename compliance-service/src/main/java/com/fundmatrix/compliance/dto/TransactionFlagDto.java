package com.fundmatrix.compliance.dto;

import com.fundmatrix.compliance.domain.enums.FlagStatus;

import java.math.BigDecimal;
import java.time.Instant;


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
