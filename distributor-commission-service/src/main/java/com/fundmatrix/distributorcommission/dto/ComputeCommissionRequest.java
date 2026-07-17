package com.fundmatrix.distributorcommission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Compute trail commission for a distributor on a scheme for a period. The trail rate is
 * the annualised percentage; the system books one month's accrual on the period's AUM.
 */
public record ComputeCommissionRequest(
        @NotNull Long distributorId,
        @NotNull Long schemeId,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}", message = "period must be in YYYY-MM format") String period,
        @NotNull @PositiveOrZero BigDecimal trailRate
) {
}
