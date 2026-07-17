package com.fundmatrix.compliance.dto;

import com.fundmatrix.compliance.domain.enums.FlagStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Move a flag through its review workflow (REVIEWED / CLEARED / ESCALATED) with an optional note. */
public record ReviewFlagRequest(
        @NotNull FlagStatus status,
        @Size(max = 500) String note
) {
}
