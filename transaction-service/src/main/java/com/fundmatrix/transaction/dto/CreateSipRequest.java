package com.fundmatrix.transaction.dto;

import com.fundmatrix.transaction.domain.enums.SipFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSipRequest(
        @NotNull Long folioId,
        @NotNull Long optionId,
        @NotNull @DecimalMin(value = "1.0") BigDecimal amount,
        @NotNull SipFrequency frequency,
        @NotNull LocalDate startDate,
        @Future LocalDate endDate,
        @Min(1) Integer instalmentCount
) {
}
