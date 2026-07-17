package com.fundmatrix.transaction.dto;

/** Mirrors auth-user-service's user summary (GET /users/{id}). */
public record UserDto(Long id, String name, String email, String role, String status) {
}
