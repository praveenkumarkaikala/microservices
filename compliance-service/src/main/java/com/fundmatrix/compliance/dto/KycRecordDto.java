package com.fundmatrix.compliance.dto;

import java.time.LocalDate;


public record KycRecordDto(
        Long investorId,
        String kycStatus,
        LocalDate verifiedDate
) {
}
