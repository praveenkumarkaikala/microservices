package com.fundmatrix.authuser.security;

import com.fundmatrix.authuser.common.exception.ResourceNotFoundException;
import com.fundmatrix.authuser.domain.User;
import com.fundmatrix.authuser.domain.enums.Role;
import com.fundmatrix.authuser.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private User principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return user;
    }

    public Long getId() {
        return principal().getId();
    }

    public String getEmail() {
        return principal().getEmail();
    }

    public Role getRole() {
        return principal().getRole();
    }

    public User requireUser() {
        Long id = getId();
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }
}
