package com.fundmatrix.transaction.dto;

import java.math.BigDecimal;

/** Mirrors fund-catalog-service's FundScheme summary (GET /schemes/{id}). */
public record SchemeDto(
        Long id,
        String schemeName,
        String schemeCode,
        String category,
        String riskProfile,
        String benchmarkIndex,
        Long fundManagerId,
        String fundManagerName,
        BigDecimal minInvestment,
        String exitLoadSlab,
        BigDecimal exitLoadRate,
        Integer exitLoadPeriodDays,
        BigDecimal expenseRatio,
        BigDecimal minSipAmount,
        BigDecimal minSwpAmount,
        String cutoffTime,
        String status
) {
}
