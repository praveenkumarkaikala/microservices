package com.fundmatrix.distributorcommission.config;

import com.fundmatrix.distributorcommission.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(c -> c.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ── PUBLIC ──
                        .requestMatchers(
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/actuator/**"
                        ).permitAll()

                        // ── DISTRIBUTORS ──
                        .requestMatchers(HttpMethod.POST, "/distributors").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/distributors/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/distributors", "/distributors/**")
                        .hasAnyAuthority("FUND_OPS", "FUND_ACCOUNTANT", "COMPLIANCE", "ADMIN")

                        // ── COMMISSIONS ──
                        .requestMatchers(HttpMethod.GET, "/commissions/mine").hasAuthority("DISTRIBUTOR")
                        .requestMatchers(HttpMethod.GET, "/commissions/distributor/**")
                        .hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/commissions/compute")
                        .hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/commissions/*/approve")
                        .hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/commissions/*/pay")
                        .hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")

                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(401);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"message\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"message\":\"You do not have permission\"}");
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
