package com.fundmatrix.transaction.dto;

/** Mirrors compliance-kyc-service's KYC status (GET /kyc/status/{investorId}). */
public record KycStatusDto(Long investorId, String kycStatus, boolean compliant) {
}
