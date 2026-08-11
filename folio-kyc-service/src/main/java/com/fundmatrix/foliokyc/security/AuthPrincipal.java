package com.fundmatrix.foliokyc.security;

import com.fundmatrix.foliokyc.domain.enums.Role;

public record AuthPrincipal(Long id, String email, Role role) {
}
