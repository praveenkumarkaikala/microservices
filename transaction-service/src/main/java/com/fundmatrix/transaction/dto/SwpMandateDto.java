package com.fundmatrix.transaction.dto;

import com.fundmatrix.transaction.domain.enums.SipFrequency;
import com.fundmatrix.transaction.domain.enums.SipStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SwpMandateDto(
        Long id,
        String mandateRef,
        Long folioId,
        String folioNumber,
        Long schemeId,
        String schemeName,
        Long optionId,
        BigDecimal amount,
        SipFrequency frequency,
        LocalDate startDate,
        LocalDate endDate,
        Integer instalmentCount,
        Integer instalmentsExecuted,
        LocalDate nextInstalmentDate,
        SipStatus status
) {
}
