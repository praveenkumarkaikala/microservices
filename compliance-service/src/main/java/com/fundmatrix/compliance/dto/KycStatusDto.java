package com.fundmatrix.compliance.dto;

/**
 * Wire shape for folio-kyc-service's GET /kyc/status/{investorId} - a single investor's
 * KYC posture. compliant mirrors folio-kyc-service's own compliant KYC-status check.
 */
public record KycStatusDto(
        Long investorId,
        String kycStatus,
        boolean compliant
) {
}
