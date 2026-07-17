package com.fundmatrix.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Place a fresh/additional purchase into a scheme option for a folio. */
public record SubscriptionRequest(
        @NotNull Long folioId,
        @NotNull Long optionId,
        @NotNull @DecimalMin(value = "1.0") BigDecimal amount
) {
}
