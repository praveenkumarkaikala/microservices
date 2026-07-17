package com.fundmatrix.navaccounting.security;

import com.fundmatrix.navaccounting.domain.enums.Role;

/**
 * Lightweight stand-in for the monolith's full User entity as the Spring Security
 * principal. Downstream (non-auth) services don't own the user table, so the
 * authenticated identity is reconstructed entirely from JWT claims - no DB round trip.
 */
public record AuthPrincipal(Long id, String email, Role role) {
}
