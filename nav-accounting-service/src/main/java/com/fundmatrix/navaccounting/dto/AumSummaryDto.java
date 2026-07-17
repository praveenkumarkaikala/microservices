package com.fundmatrix.navaccounting.dto;

import java.math.BigDecimal;

/** category is FundScheme's SchemeCategory enum name as a string (that enum is owned by fund-catalog-service). */
public record AumSummaryDto(
        Long schemeId,
        String schemeName,
        String schemeCode,
        String category,
        BigDecimal latestNav,
        BigDecimal totalAum,
        BigDecimal totalUnitsOutstanding
) {
}
