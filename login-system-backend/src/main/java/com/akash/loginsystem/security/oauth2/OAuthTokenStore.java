package com.akash.loginsystem.security.oauth2;

import com.akash.loginsystem.dto.response.AuthResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived in-memory store for OAuth2 post-login token exchange.
 *
 * Instead of putting JWTs in the redirect URL (CWE-598), the success handler
 * stores the AuthResponse here keyed by an opaque one-time code (30 s TTL).
 * The React frontend calls POST /api/v1/auth/oauth2/token with that code to
 * receive the actual tokens — never exposed in browser history or server logs.
 */
@Component
@Slf4j
public class OAuthTokenStore {

    private static final long TTL_SECONDS = 30;

    private record Entry(AuthResponse response, Instant expiresAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /** Stores the AuthResponse and returns the opaque one-time code. */
    public String store(AuthResponse response) {
        String code = UUID.randomUUID().toString();
        store.put(code, new Entry(response, Instant.now().plusSeconds(TTL_SECONDS)));
        return code;
    }

    /**
     * Consumes the code (removes it) and returns the AuthResponse.
     * Returns empty if the code is unknown or expired.
     */
    public Optional<AuthResponse> consume(String code) {
        Entry entry = store.remove(code);
        if (entry == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            log.warn("OAuth code expired: {}", code);
            return Optional.empty();
        }
        return Optional.of(entry.response());
    }

    /** Evict stale entries every 60 seconds to prevent unbounded growth. */
    @Scheduled(fixedDelay = 60_000)
    public void evictExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }
}
