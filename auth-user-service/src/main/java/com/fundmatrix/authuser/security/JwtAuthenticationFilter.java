package com.fundmatrix.authuser.security;

import com.fundmatrix.authuser.domain.enums.UserStatus;
import com.fundmatrix.authuser.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String token = header.substring(BEARER.length());
                if (jwtService.isValid(token)) {
                    userRepository.findByEmailIgnoreCase(jwtService.extractUsername(token))
                            .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                            .ifPresent(user -> {
                                var authorities = List.of(new SimpleGrantedAuthority(user.getRole().name()));
                                var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
                                SecurityContextHolder.getContext().setAuthentication(auth);
                            });
                }
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
