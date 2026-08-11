package com.fundmatrix.compliance.security;

import com.fundmatrix.compliance.domain.enums.Role;
 record AuthPrincipal(Long id, String email, Role role) {
}
