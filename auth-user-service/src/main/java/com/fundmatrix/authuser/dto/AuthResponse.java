package com.fundmatrix.authuser.dto;

import java.time.Instant;

public record AuthResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        UserDto user
) {
    public static AuthResponse bearer(String token, Instant expiresAt, UserDto user) {
        return new AuthResponse(token, "Bearer", expiresAt, user);
    }
}
