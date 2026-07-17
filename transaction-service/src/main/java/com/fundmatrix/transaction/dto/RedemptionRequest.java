package com.fundmatrix.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Redeem units from a holding. Provide either a unit quantity or a redemption amount;
 * units take precedence when both are supplied. Use {@code redeemAll} to liquidate fully.
 */
public record RedemptionRequest(
        @NotNull Long folioId,
        @NotNull Long optionId,
        @DecimalMin(value = "0.0001") BigDecimal units,
        @DecimalMin(value = "1.0") BigDecimal amount,
        boolean redeemAll
) {
}
