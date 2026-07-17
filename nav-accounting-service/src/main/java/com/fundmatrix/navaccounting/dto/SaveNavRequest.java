package com.fundmatrix.navaccounting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaveNavRequest(
        @NotNull Long optionId,
        @NotNull LocalDate navDate,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal navValue,
        @PositiveOrZero BigDecimal totalAum,
        @PositiveOrZero BigDecimal totalUnitsOutstanding
) {
}
