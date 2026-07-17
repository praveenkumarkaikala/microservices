package com.fundmatrix.navaccounting.client;

import java.math.BigDecimal;

/** Mirrors fund-catalog-service's GET /schemes/{id} response (FEIGN_CONTRACTS.md). */
public record SchemeDto(
        Long id,
        String schemeName,
        String schemeCode,
        String category,
        BigDecimal minInvestment,
        BigDecimal exitLoadRate,
        BigDecimal expenseRatio,
        String cutoffTime,
        String status
) {
}
