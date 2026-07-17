package com.fundmatrix.dashboard.client;

import java.math.BigDecimal;

/** Mirrors the SchemeOptionDto fields specified in FEIGN_CONTRACTS.md (fund-catalog-service section). */
public record SchemeOptionDto(
        Long schemeId,
        String schemeName,
        String optionType,
        String optionStatus,
        String schemeStatus,
        String schemeCategory,
        String cutoffTime,
        BigDecimal exitLoadRate,
        BigDecimal minInvestment
) {
}
