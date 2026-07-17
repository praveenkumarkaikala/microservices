package com.fundmatrix.navaccounting.dto;

import java.math.BigDecimal;

public record ExpenseComplianceDto(
        Long schemeId,
        String schemeName,
        BigDecimal expenseRatioLimit,
        BigDecimal chargedRate,
        BigDecimal utilisationPct,
        String status
) {
}
