package com.fundmatrix.transaction.dto;

import com.fundmatrix.transaction.domain.enums.SipFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Editable fields of an SWP mandate (all optional — only non-null values are applied). */
public record UpdateSwpRequest(
        @DecimalMin(value = "1.0") BigDecimal amount,
        SipFrequency frequency,
        LocalDate endDate,
        @Min(1) Integer instalmentCount
) {
}
