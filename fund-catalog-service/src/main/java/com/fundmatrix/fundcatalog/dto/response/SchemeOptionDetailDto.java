package com.fundmatrix.fundcatalog.dto.response;

import com.fundmatrix.fundcatalog.domain.enums.OptionStatus;
import com.fundmatrix.fundcatalog.domain.enums.OptionType;
import com.fundmatrix.fundcatalog.domain.enums.SchemeCategory;
import com.fundmatrix.fundcatalog.domain.enums.SchemeStatus;

import java.math.BigDecimal;

/**
 * Service-to-service read model for GET /schemes/options/{optionId} (per FEIGN_CONTRACTS.md
 * "fund-catalog-service" section). Bundles scheme-level fields alongside the option so
 * consumers (folio-transaction, nav-accounting, distributor-commission, dashboard) don't need a
 * second call just to learn the parent scheme's status/category/cutoff/exit-load/min-investment.
 */
public record SchemeOptionDetailDto(
        Long id,
        Long schemeId,
        String schemeName,
        OptionType optionType,
        String isin,
        OptionStatus optionStatus,
        SchemeStatus schemeStatus,
        SchemeCategory schemeCategory,
        String cutoffTime,
        BigDecimal exitLoadRate,
        BigDecimal minInvestment
) {
}
