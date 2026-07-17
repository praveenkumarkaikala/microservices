package com.fundmatrix.transaction.dto;

import com.fundmatrix.transaction.domain.enums.SipFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSwpRequest(
        @NotNull Long folioId,
        @NotNull Long optionId,
        @NotNull @DecimalMin(value = "1.0", message = "Minimum withdrawal amount is 1") BigDecimal amount,
        @NotNull SipFrequency frequency,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @Min(1) Integer instalmentCount
) {
}
