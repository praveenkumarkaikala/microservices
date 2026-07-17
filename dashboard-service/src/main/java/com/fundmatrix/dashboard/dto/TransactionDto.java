package com.fundmatrix.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Mirrors folio-transaction-service's TransactionDto shape (own copy - no shared JAR). */
public record TransactionDto(
        Long id,
        String transactionRef,
        Long folioId,
        String folioNumber,
        Long schemeId,
        String schemeName,
        Long optionId,
        String optionType,
        String transactionType,
        BigDecimal amount,
        BigDecimal units,
        BigDecimal applicableNav,
        Instant transactionDate,
        String cutOffStatus,
        String status,
        BigDecimal exitLoadAmount,
        String remarks
) {
}
