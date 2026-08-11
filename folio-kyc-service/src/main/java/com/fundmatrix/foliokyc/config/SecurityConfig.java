package com.fundmatrix.foliokyc.config;

import com.fundmatrix.foliokyc.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Merges the old folio-transaction-service's folio/holdings rules with the old
 * compliance-kyc-service's KYC rules, since both now live in this one module.
 */
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
                        
                        .requestMatchers(
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/actuator/**"
                        ).permitAll()

                       
                        .requestMatchers(HttpMethod.POST, "/folios")
                            .hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN")
                            
                            .requestMatchers(HttpMethod.GET, "/holdings/**")
                            .hasAnyAuthority("INVESTOR", "DISTRIBUTOR", "FUND_OPS", "ADMIN","FUND_ACCOUNTANT")

                       
                        .requestMatchers(HttpMethod.PATCH, "/folios/*/status")
                            .hasAnyAuthority("FUND_OPS", "ADMIN")

                       
                        .requestMatchers(HttpMethod.GET, "/kyc/mine").hasAuthority("INVESTOR")
                        .requestMatchers(HttpMethod.POST, "/kyc").hasAuthority("INVESTOR")
                        .requestMatchers(HttpMethod.GET, "/kyc", "/kyc/investor/**")
                                .hasAnyAuthority("FUND_OPS", "COMPLIANCE", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/kyc/*/status")
                                .hasAnyAuthority("FUND_OPS", "COMPLIANCE", "ADMIN")

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
