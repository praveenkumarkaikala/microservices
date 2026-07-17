package com.fundmatrix.fundcatalog.dto;

import com.fundmatrix.fundcatalog.domain.enums.RiskProfile;
import com.fundmatrix.fundcatalog.domain.enums.SchemeCategory;
import com.fundmatrix.fundcatalog.domain.enums.SchemeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;


public record SaveSchemeRequest(
        @NotBlank @Size(max = 160) String schemeName,
        @NotBlank @Size(max = 40) String schemeCode,
        @NotNull SchemeCategory category,
        @NotNull RiskProfile riskProfile,
        @Size(max = 120) String benchmarkIndex,
        Long fundManagerId,
        @Size(max = 120) String fundManagerName,
        @PositiveOrZero BigDecimal minInvestment,
        @Size(max = 255) String exitLoadSlab,
        @PositiveOrZero BigDecimal exitLoadRate,
        @PositiveOrZero Integer exitLoadPeriodDays,
        @PositiveOrZero BigDecimal expenseRatio,
        @PositiveOrZero BigDecimal minSipAmount,
        @PositiveOrZero BigDecimal minSwpAmount,
        @Size(max = 5) String cutoffTime,
        SchemeStatus status
) {
}
