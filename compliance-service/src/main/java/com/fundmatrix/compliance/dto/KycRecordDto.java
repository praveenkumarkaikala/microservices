package com.fundmatrix.compliance.dto;

import java.time.LocalDate;

/**
 * Wire shape for folio-kyc-service's GET /kyc listing endpoint, trimmed to just the fields
 * ComplianceService.kycStatus()/generateReport() need to tally KYC posture across all
 * investors. kycStatus is the KycStatus enum name as a string ("COMPLIANT", "PENDING",
 * "NON_COMPLIANT", "EXPIRED") - compliance-service does not own that enum, so it is not
 * duplicated as a type here, just compared by string value.
 */
public record KycRecordDto(
        Long investorId,
        String kycStatus,
        LocalDate verifiedDate
) {
}
