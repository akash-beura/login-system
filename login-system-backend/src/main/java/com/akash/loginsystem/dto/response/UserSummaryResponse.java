package com.akash.loginsystem.dto.response;

import com.akash.loginsystem.entity.User;
import com.akash.loginsystem.model.AuthProvider;

import java.util.UUID;

/**
 * Minimal user identity embedded in every auth response (login, register, refresh, OAuth).
 * Profile data (address, phone) is excluded here — those fields belong on a dedicated
 * /profile endpoint, not on every token issuance response.
 *
 * Using a Java record ensures clean Jackson serialization/deserialization without
 * additional annotations — required for Redis-based OAuthTokenStore storage.
 */
public record UserSummaryResponse(
    UUID id,
    String email,
    String name,
    AuthProvider provider,
    boolean passwordSet
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getProvider(),
            user.isPasswordSet()
        );
    }
}
