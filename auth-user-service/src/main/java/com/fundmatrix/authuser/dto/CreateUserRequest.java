package com.fundmatrix.authuser.dto;

import com.fundmatrix.authuser.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin-driven user provisioning for staff and distributor accounts. */
public record CreateUserRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email String email,
        @Size(max = 20) String phone,
        @NotNull Role role,
        @NotBlank @Size(min = 6, max = 72) String password
) {
}
