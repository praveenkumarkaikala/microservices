package com.fundmatrix.compliance.dto;

import java.math.BigDecimal;
import java.time.Instant;


public record ComplianceReportDto(
        Instant generatedAt,
        String generatedBy,
        ComplianceKycStatusDto kyc,
        long flagsOpen,
        long flagsReviewed,
        long flagsCleared,
        long flagsEscalated,
        long flaggedTotalCount,
        BigDecimal flaggedTotalAmount
) {
}
