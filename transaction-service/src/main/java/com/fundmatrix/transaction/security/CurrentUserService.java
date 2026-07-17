package com.fundmatrix.transaction.security;

import com.fundmatrix.transaction.domain.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private AuthPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return principal;
    }

    public Long getId() {
        return principal().id();
    }

    public String getEmail() {
        return principal().email();
    }

    public Role getRole() {
        return principal().role();
    }
}
