package com.fundmatrix.navaccounting.client;

import java.math.BigDecimal;

/** Mirrors fund-catalog-service's GET /schemes/options/{optionId} response (FEIGN_CONTRACTS.md). */
public record SchemeOptionDto(
        Long id,
        String isin,
        String optionType,
        String optionStatus,
        Long schemeId,
        String schemeName,
        String schemeCategory,
        String schemeStatus,
        String cutoffTime,
        BigDecimal exitLoadRate,
        BigDecimal minInvestment
) {
}
