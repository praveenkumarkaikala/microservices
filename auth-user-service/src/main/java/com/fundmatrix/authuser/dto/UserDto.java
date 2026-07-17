package com.fundmatrix.authuser.dto;

import com.fundmatrix.authuser.domain.enums.Role;
import com.fundmatrix.authuser.domain.enums.UserStatus;

import java.time.Instant;

public record UserDto(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        UserStatus status,
        Instant createdAt
) {
}
