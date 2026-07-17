package com.fundmatrix.distributorcommission.client;

/** Mirrors auth-user-service's UserDto, as returned by GET /users/{id}. */
public record UserDto(
        Long id,
        String name,
        String email,
        String phone,
        String role,
        String status,
        java.time.Instant createdAt
) {
}
