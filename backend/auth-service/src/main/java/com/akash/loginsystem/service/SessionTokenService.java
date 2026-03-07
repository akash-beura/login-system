package com.akash.loginsystem.service;

import com.akash.loginsystem.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis-backed session tokens with sliding TTL.
 * Each session is stored as "session:{token}" → userId.
 */
@Service
@RequiredArgsConstructor
public class SessionTokenService {

    private static final String KEY_PREFIX = "session:";

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;

    /** Creates a new session token in Redis. Returns the token string. */
    public String createSession(UUID userId) {
        String token = UUID.randomUUID().toString();
        Duration ttl = Duration.ofMinutes(appProperties.getSession().getTimeoutMinutes());
        redisTemplate.opsForValue().set(KEY_PREFIX + token, userId.toString(), ttl);
        return token;
    }

    /** Validates the session and resets TTL (sliding window). Returns userId or null. */
    public String validateAndRefresh(String sessionToken) {
        if (sessionToken == null) {
            return null;
        }
        String key = KEY_PREFIX + sessionToken;
        String userId = redisTemplate.opsForValue().get(key);
        if (userId != null) {
            Duration ttl = Duration.ofMinutes(appProperties.getSession().getTimeoutMinutes());
            redisTemplate.expire(key, ttl);
        }
        return userId;
    }

    /** Deletes the session from Redis. */
    public void invalidate(String sessionToken) {
        if (sessionToken != null) {
            redisTemplate.delete(KEY_PREFIX + sessionToken);
        }
    }
}
