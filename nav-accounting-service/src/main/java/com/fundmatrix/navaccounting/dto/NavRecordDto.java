package com.fundmatrix.navaccounting.dto;

import com.fundmatrix.navaccounting.domain.enums.NavStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NavRecordDto(
        Long id,
        Long schemeId,
        String schemeName,
        Long optionId,
        String optionType,
        LocalDate navDate,
        BigDecimal navValue,
        BigDecimal totalAum,
        BigDecimal totalUnitsOutstanding,
        Long publishedById,
        NavStatus status
) {
}
