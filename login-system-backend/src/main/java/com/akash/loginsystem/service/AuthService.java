package com.akash.loginsystem.service;

import com.akash.loginsystem.dto.request.LoginRequest;
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
     * Set password for the first time (account-linking flow).
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
}
