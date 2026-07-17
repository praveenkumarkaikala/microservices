package com.fundmatrix.transaction.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Forwards the caller's Authorization bearer token onto every outbound Feign call, so the
 * downstream service's own JwtAuthenticationFilter + hasAuthority rules authorize the
 * ORIGINAL user - not a separate service-account. This is what lets internal endpoints
 * reuse the exact same hasAuthority mapping instead of needing a parallel trust model.
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String header = attrs.getRequest().getHeader("Authorization");
                if (header != null) {
                    requestTemplate.header("Authorization", header);
                }
            }
        };
    }
}
