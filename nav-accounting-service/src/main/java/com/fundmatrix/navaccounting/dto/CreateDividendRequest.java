package com.fundmatrix.navaccounting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateDividendRequest(
        @NotNull Long optionId,
        @NotNull LocalDate recordDate,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal dividendPerUnit
) {
}
