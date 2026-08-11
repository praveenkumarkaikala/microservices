package com.fundmatrix.gateway.security;

import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER = "Bearer ";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator"
    );

    private final JwtSupport jwtSupport;

    public JwtAuthenticationGlobalFilter(JwtSupport jwtSupport) {
        this.jwtSupport = jwtSupport;
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        System.out.print(isPublic(path));
        if (isPublic(path)) {
        	
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String header = request.getHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            return unauthorized(exchange, "Authentication required");
        }

        String token = header.substring(BEARER.length());
        if (!jwtSupport.isValid(token)) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        Claims claims = jwtSupport.parse(token);
        ServerHttpRequest mutated = request.mutate()
                .header("X-Auth-User-Email", claims.getSubject())
                .header("X-Auth-User-Id", String.valueOf(claims.get("uid")))
                .header("X-Auth-User-Role", String.valueOf(claims.get("role")))
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        byte[] bytes = ("{\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
