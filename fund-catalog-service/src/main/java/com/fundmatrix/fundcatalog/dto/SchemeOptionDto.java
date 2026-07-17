package com.fundmatrix.fundcatalog.dto;

import com.fundmatrix.fundcatalog.domain.enums.OptionStatus;
import com.fundmatrix.fundcatalog.domain.enums.OptionType;

import java.math.BigDecimal;

/**
 * NOTE: {@code latestNav} intentionally dropped (see FundSchemeDto for rationale) - this is now a
 * pure catalogue read model. Downstream services fetch NAV from nav-accounting-service directly.
 */
public record SchemeOptionDto(
        Long id,
        Long schemeId,
        String schemeName,
        OptionType optionType,
        String isin,
        OptionStatus status
) {
}
