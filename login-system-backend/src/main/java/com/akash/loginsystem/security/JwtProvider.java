package com.akash.loginsystem.security;

import com.akash.loginsystem.config.AppProperties;
import com.akash.loginsystem.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Handles JWT generation and validation.
 * Claims: userId, email, role.
 * Algorithm: HS256.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtProvider {

    private final AppProperties appProperties;

    /** S-5: Derived once at startup — avoids Base64 decode + key construction on every call. */
    private SecretKey cachedSigningKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = Decoders.BASE64.decode(appProperties.getJwt().getSecret());
        cachedSigningKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Token Generation ────────────────────────────────────────────────────

    public String generateAccessToken(User user) {
        return buildToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name(),
                appProperties.getJwt().getExpiryMs()
        );
    }

    public String generateRefreshTokenValue() {
        // Opaque random string — stored in DB, not a JWT
        return UUID.randomUUID().toString();
    }

    // ── Validation ───────────────────────────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
        }
        return false;
    }

    // ── Extraction ────────────────────────────────────────────────────────────

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private String buildToken(String subject, String email, String role, long expiryMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMs))
                .signWith(cachedSigningKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(cachedSigningKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
