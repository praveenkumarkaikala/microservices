package com.fundmatrix.transaction.config;

import com.fundmatrix.transaction.security.JwtAuthenticationFilter;
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

                        // ── INVESTOR / DISTRIBUTOR / FUND_OPS / ADMIN ──
                        .requestMatchers(HttpMethod.POST, "/transactions/subscriptions")
                            .hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/transactions/redemptions")
                            .hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/transactions/switches")
                            .hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/sips")
                            .hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/sips/*/pause")
                            .hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/sips/*/resume")
                            .hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/sips/*/cancel")
                            .hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/swp-mandates")
                            .hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/swp-mandates/*")
                            .hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/swp-mandates/*/status")
                            .hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")

                        // ── FUND_OPS / ADMIN ──
                        .requestMatchers(HttpMethod.GET, "/transactions/queue")
                            .hasAnyAuthority("FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/transactions/allot-batch")
                            .hasAnyAuthority("FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/transactions/*/accept")
                            .hasAnyAuthority("FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/transactions/*/allot")
                            .hasAnyAuthority("FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/transactions/*/reject")
                            .hasAnyAuthority("FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/sips/due")
                            .hasAnyAuthority("FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/sips/*/run")
                            .hasAnyAuthority("FUND_OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/swp-mandates/*/process")
                            .hasAnyAuthority("FUND_OPS", "ADMIN")

                        // ── COMPLIANCE / ADMIN ──
                        .requestMatchers(HttpMethod.GET, "/transactions/flagged")
                            .hasAnyAuthority("COMPLIANCE", "ADMIN")

                        // ── everything else (incl. /transactions/flags**, called with the
                        // original caller's forwarded token) ──
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
