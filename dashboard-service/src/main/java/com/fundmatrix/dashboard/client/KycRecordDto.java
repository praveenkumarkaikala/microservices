package com.fundmatrix.dashboard.client;

/** Own lean copy of compliance-kyc-service's KycRecordDto - only the fields dashboard-service needs. */
public record KycRecordDto(Long id, Long investorId, String kycStatus) {
}
