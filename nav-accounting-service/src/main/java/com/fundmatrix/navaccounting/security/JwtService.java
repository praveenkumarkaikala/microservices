package com.fundmatrix.navaccounting.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Validates JWTs issued by auth-user-service. Every microservice shares the same HMAC
 * secret (fundmatrix.jwt.secret, delivered by the config server) so a single token
 * works everywhere - only auth-user-service also implements generateToken(User).
 */
@Service
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(@Value("${fundmatrix.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object uid = parse(token).get("uid");
        return uid == null ? null : Long.valueOf(String.valueOf(uid));
    }

    public String extractRole(String token) {
        return String.valueOf(parse(token).get("role"));
    }

    public boolean isValid(String token) {
        try {
            return parse(token).getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
