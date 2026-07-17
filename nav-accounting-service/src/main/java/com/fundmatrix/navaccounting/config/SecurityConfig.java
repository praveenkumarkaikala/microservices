package com.fundmatrix.navaccounting.config;

import com.fundmatrix.navaccounting.security.JwtAuthenticationFilter;
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

                        // ── NAV ──
                        .requestMatchers(HttpMethod.POST, "/nav").hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/nav/*/publish").hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/nav/aum-summary")
                                .hasAnyAuthority("FUND_ACCOUNTANT", "COMPLIANCE", "ADMIN")

                        // ── EXPENSE ACCRUALS (all methods) ──
                        .requestMatchers("/accruals/**").hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")

                        // ── DIVIDENDS ──
                        .requestMatchers(HttpMethod.GET, "/dividends/entitlements/mine").hasAuthority("INVESTOR")
                        .requestMatchers(HttpMethod.GET, "/dividends/*/entitlements")
                                .hasAnyAuthority("FUND_ACCOUNTANT", "COMPLIANCE", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/dividends")
                                .hasAnyAuthority("FUND_ACCOUNTANT", "COMPLIANCE", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/dividends").hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/dividends/*/compute").hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/dividends/*/approve").hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/dividends/*/process").hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/dividends/*/cancel").hasAnyAuthority("FUND_ACCOUNTANT", "ADMIN")

                        // Everything else (including the internal /nav/published/{id}, /nav/latest/{id}
                        // lookups used by folio-transaction-service) just needs a valid, authenticated caller.
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
