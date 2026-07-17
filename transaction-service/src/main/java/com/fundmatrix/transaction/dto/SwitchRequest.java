package com.fundmatrix.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Switch units from a source scheme option into a target option within the same folio. */
public record SwitchRequest(
        @NotNull Long folioId,
        @NotNull Long fromOptionId,
        @NotNull Long toOptionId,
        @DecimalMin(value = "0.0001") BigDecimal units,
        boolean switchAll
) {
}
