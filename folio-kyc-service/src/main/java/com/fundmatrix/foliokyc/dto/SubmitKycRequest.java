package com.fundmatrix.foliokyc.dto;

import com.fundmatrix.foliokyc.domain.enums.KycType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Investor self-submission of KYC details. The investor is taken from the authenticated
 * user, so no investorId is accepted here - investors submit their own KYC only.
 */
public record SubmitKycRequest(
        @NotNull KycType kycType,
        @NotBlank @Size(max = 60) String documentType,
        @NotBlank @Size(max = 60) String documentRef
) {
}
