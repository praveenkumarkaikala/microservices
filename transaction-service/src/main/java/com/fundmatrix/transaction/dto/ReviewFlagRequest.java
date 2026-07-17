package com.fundmatrix.transaction.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewFlagRequest(@NotBlank String status) {
}
