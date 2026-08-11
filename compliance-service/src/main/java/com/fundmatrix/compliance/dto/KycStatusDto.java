package com.fundmatrix.compliance.dto;


public record KycStatusDto(
        Long investorId,
        String kycStatus,
        boolean compliant
) {
}
