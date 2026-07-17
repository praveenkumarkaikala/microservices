package com.fundmatrix.audit.security;

import com.fundmatrix.audit.domain.enums.Role;

/**
 * Lightweight stand-in for the monolith's full User entity as the Spring Security
 * principal. This service doesn't own the user table, so the authenticated identity is
 * reconstructed entirely from JWT claims - no DB round trip. Note: the actual audit write
 * (AuditService.record) trusts the actorId/actorRole/actorEmail fields on the request body
 * instead of this principal, since those are explicitly supplied by the calling service.
 */
public record AuthPrincipal(Long id, String email, Role role) {
}
