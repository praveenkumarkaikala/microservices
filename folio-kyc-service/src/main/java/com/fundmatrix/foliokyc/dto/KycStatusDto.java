package com.fundmatrix.foliokyc.dto;

/**
 * Internal, cross-service KYC posture lookup for a single investor. Consumed by
 * transaction-service (pre-transaction KYC gate), nav-accounting-service,
 * distributor-commission-service, compliance-service and dashboard-service.
 */
public record KycStatusDto(
        Long investorId,
        String kycStatus,
        boolean compliant
) {
}
