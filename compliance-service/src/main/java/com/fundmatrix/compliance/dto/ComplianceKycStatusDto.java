package com.fundmatrix.compliance.dto;

public record ComplianceKycStatusDto(
        long compliant,
        long pending,
        long nonCompliant,
        long expired,
        long total
) {
}
