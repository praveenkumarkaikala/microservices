package com.fundmatrix.dashboard.client;

/** Mirrors the KycStatusDto specified in FEIGN_CONTRACTS.md (compliance-kyc-service section). */
public record KycStatusDto(Long investorId, String kycStatus, boolean compliant) {
}
