package com.fundmatrix.navaccounting.dto;

import com.fundmatrix.navaccounting.domain.enums.ExpenseType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateAccrualRequest(
        @NotNull Long schemeId,
        @NotNull ExpenseType expenseType,
        @NotNull @PositiveOrZero BigDecimal annualisedRate,
        BigDecimal accrualAmount,
        LocalDate accrualDate
) {
}
