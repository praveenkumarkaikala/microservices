package com.fundmatrix.transaction.dto;

import java.math.BigDecimal;

/** Mirrors fund-catalog-service's SchemeOption + scheme summary (GET /schemes/options/{optionId}). */
public record SchemeOptionDto(
        Long id,
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
