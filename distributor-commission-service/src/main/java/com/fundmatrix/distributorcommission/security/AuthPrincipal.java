package com.fundmatrix.distributorcommission.security;

import com.fundmatrix.distributorcommission.domain.enums.Role;

/**
 * Lightweight stand-in for the monolith's full User entity as the Spring Security
 * principal. This service doesn't own the user table, so the authenticated identity is
 * reconstructed entirely from JWT claims - no DB round trip.
 */
public record AuthPrincipal(Long id, String email, Role role) {
}
