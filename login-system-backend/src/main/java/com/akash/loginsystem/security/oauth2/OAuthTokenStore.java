package com.akash.loginsystem.security.oauth2;

import com.akash.loginsystem.dto.response.AuthResponse;
import com.akash.loginsystem.dto.response.UserSummaryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed store for OAuth2 post-login token exchange.
 *
 * H-1 FIX: Replaced ConcurrentHashMap with Redis so the store is shared across
 * all backend instances (horizontal scaling). Redis TTL handles expiry natively —
 * no @Scheduled cleanup needed.
 *
 * getAndDelete() is atomic — prevents two concurrent consumers from retrieving
 * the same code, eliminating the race condition present in the previous implementation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuthTokenStore {

    private static final Duration TTL = Duration.ofSeconds(30);
    private static final String KEY_PREFIX = "oauth:code:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Internal serialization contract.
     * Java records deserialize cleanly with Jackson without extra annotations.
     */
    private record OAuthCodeData(
        String accessToken,
        String refreshToken,
        boolean requiresPasswordSet,
        UserSummaryResponse user
    ) {}

    /** Stores the AuthResponse under a new opaque one-time code and returns that code. */
    public String store(AuthResponse response) {
        String code = UUID.randomUUID().toString();
        OAuthCodeData data = new OAuthCodeData(
            response.getAccessToken(),
            response.getRefreshToken(),
            response.isRequiresPasswordSet(),
            response.getUser()
        );
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(KEY_PREFIX + code, json, TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize OAuth code data", e);
        }
        return code;
    }

    /**
     * Atomically consumes the code (getAndDelete) and returns the AuthResponse.
     * Returns empty if the code is unknown, already consumed, or TTL-expired.
     */
    public Optional<AuthResponse> consume(String code) {
        String json = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + code);
        if (json == null) {
            log.warn("OAuth code not found or already consumed: {}", code);
            return Optional.empty();
        }
        try {
            OAuthCodeData data = objectMapper.readValue(json, OAuthCodeData.class);
            return Optional.of(AuthResponse.builder()
                .accessToken(data.accessToken())
                .refreshToken(data.refreshToken())
                .requiresPasswordSet(data.requiresPasswordSet())
                .user(data.user())
                .build());
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize OAuth code data for code: {}", code, e);
            return Optional.empty();
        }
    }
}
