package com.fundmatrix.navaccounting.dto;

import com.fundmatrix.navaccounting.domain.enums.ExpenseStatus;
import com.fundmatrix.navaccounting.domain.enums.ExpenseType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseAccrualDto(
        Long id,
        Long schemeId,
        String schemeName,
        ExpenseType expenseType,
        BigDecimal accrualAmount,
        LocalDate accrualDate,
        BigDecimal annualisedRate,
        ExpenseStatus status,
        String reversalReason
) {
}
