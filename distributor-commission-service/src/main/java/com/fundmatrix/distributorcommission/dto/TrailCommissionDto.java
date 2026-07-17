package com.fundmatrix.distributorcommission.dto;

import com.fundmatrix.distributorcommission.domain.enums.CommissionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TrailCommissionDto(
        Long id,
        Long distributorId,
        String distributorName,
        Long schemeId,
        String schemeName,
        String period,
        BigDecimal aumManaged,
        BigDecimal trailRate,
        BigDecimal commissionAmount,
        LocalDate payoutDate,
        CommissionStatus status
) {
}
