package com.fundmatrix.dashboard.client;

/** Own lean copy of auth-user-service's UserDto (FEIGN_CONTRACTS.md) - no shared JAR across services. */
public record UserDto(Long id, String name, String email, String role, String status) {
}
