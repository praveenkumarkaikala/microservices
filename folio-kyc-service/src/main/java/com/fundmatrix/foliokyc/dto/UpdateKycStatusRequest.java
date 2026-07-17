package com.fundmatrix.foliokyc.dto;

import com.fundmatrix.foliokyc.domain.enums.KycStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateKycStatusRequest(@NotNull KycStatus kycStatus) {
}
