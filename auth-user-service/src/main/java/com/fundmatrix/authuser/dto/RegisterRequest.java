package com.fundmatrix.authuser.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email String email,
        @Size(max = 10) String phone,
        @NotBlank @Size(min = 6, max = 72) String password
) {
}
