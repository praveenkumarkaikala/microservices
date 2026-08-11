package com.fundmatrix.compliance.dto;

import com.fundmatrix.compliance.domain.enums.FlagStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record ReviewFlagRequest(
        @NotNull FlagStatus status,
        @Size(max = 500) String note
) {
}
