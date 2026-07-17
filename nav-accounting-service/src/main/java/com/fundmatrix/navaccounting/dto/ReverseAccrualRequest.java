package com.fundmatrix.navaccounting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReverseAccrualRequest(
        @NotBlank @Size(max = 255) String reason
) {
}
