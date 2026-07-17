package com.fundmatrix.compliance.dto;

/** KYC compliance posture across all investors, tallied from folio-kyc-service's KYC records. */
public record ComplianceKycStatusDto(
        long compliant,
        long pending,
        long nonCompliant,
        long expired,
        long total
) {
}
