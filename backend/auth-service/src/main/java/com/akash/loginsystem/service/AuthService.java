package com.akash.loginsystem.service;

import com.akash.loginsystem.dto.request.LoginRequest;
import com.akash.loginsystem.dto.request.OAuthCodeRequest;
import com.akash.loginsystem.dto.request.RefreshRequest;
import com.akash.loginsystem.dto.request.RegisterRequest;
import com.akash.loginsystem.dto.request.SetPasswordRequest;
import com.akash.loginsystem.dto.response.AuthResponse;
import com.akash.loginsystem.entity.User;

import java.util.UUID;

public interface AuthService {

    /** Register a new LOCAL user. Throws UserAlreadyExistsException if email taken. */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticate by email + password.
     * If user exists but passwordSet=false (OAuth user), returns AuthResponse
     * with requiresPasswordSet=true and NO tokens — client must re-authenticate via Google OAuth.
     */
    AuthResponse login(LoginRequest request);

    /**
     * Account-linking flow: set password for the first time.
     * Only valid when user.passwordSet=false. Throws PasswordAlreadySetException if already set.
     * Flips passwordSet=true. Requires authenticated user (Bearer token from OAuth redirect).
     */
    AuthResponse setPassword(UUID userId, SetPasswordRequest request);

    /**
     * Issues a full token pair for a user who authenticated via OAuth2.
     * Called by OAuth2SuccessHandler after Google login succeeds.
     */
    AuthResponse completeOAuthLogin(User user);

    /**
     * Validates the refresh token, rotates it, and issues a new access + refresh pair.
     * Throws InvalidCredentialsException if the token is unknown or expired.
     */
    AuthResponse refresh(RefreshRequest request);

    /**
     * Exchanges the one-time opaque OAuth2 code for the stored AuthResponse.
     * Throws OAuthCodeExpiredException if the code is unknown, already consumed, or past its 30s TTL.
     */
    AuthResponse exchangeOAuthCode(OAuthCodeRequest request);

    /**
     * Invalidates all refresh tokens for the given user (logout / session termination).
     * The access token remains valid until its natural expiry.
     */
    void logout(UUID userId);
}
