package com.akash.loginsystem.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Returned on successful login, registration, or token refresh.
 *
 * refreshToken field is NOT serialized to JSON — it's set as an HttpOnly cookie by the backend.
 * Frontend never sees or manipulates the refresh token.
 * The field exists internally for the controller to read before setting the cookie.
 *
 * requiresPasswordSet=true signals the frontend to redirect to /set-password
 * (account-linking flow for OAuth users on first email login).
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;

    /** NOT serialized to JSON. Used internally by controller to set HttpOnly cookie. */
    @JsonIgnore
    private String refreshToken;

    /** True when an OAuth-registered user logs in with email before setting a password. */
    @Builder.Default
    private boolean requiresPasswordSet = false;

    private UserSummaryResponse user;
}
