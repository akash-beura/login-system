package com.akash.loginsystem.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Returned on successful login, registration, or token refresh.
 *
 * requiresPasswordSet=true signals the frontend to redirect to /set-password
 * (account-linking flow for OAuth users on first email login).
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    /** True when an OAuth-registered user logs in with email before setting a password. */
    @Builder.Default
    private boolean requiresPasswordSet = false;

    private UserResponse user;
}
