package com.akash.loginsystem.controller;

import com.akash.loginsystem.dto.request.LoginRequest;
import com.akash.loginsystem.dto.request.OAuthCodeRequest;
import com.akash.loginsystem.dto.request.RefreshRequest;
import com.akash.loginsystem.dto.request.RegisterRequest;
import com.akash.loginsystem.dto.request.SetPasswordRequest;
import com.akash.loginsystem.dto.response.AuthResponse;
import com.akash.loginsystem.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/register
     * Creates a new LOCAL user and returns tokens.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * POST /api/v1/auth/login
     * Authenticates by email + password.
     *
     * If passwordSet=false (OAuth-only account), returns:
     *   { requiresPasswordSet: true, user: {...} }   ← NO tokens
     * Client must re-authenticate via Google OAuth to obtain a token,
     * then call /set-password to complete account linking.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * POST /api/v1/auth/set-password
     * Account-linking flow — first-time password set for OAuth users (passwordSet=false).
     * Blocked with 409 if the account already has a password set.
     * Requires Bearer token obtained via Google OAuth redirect + /oauth2/token exchange.
     */
    @PostMapping("/set-password")
    public ResponseEntity<AuthResponse> setPassword(
            @Valid @RequestBody SetPasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(authService.setPassword(userId, request));
    }

    /**
     * POST /api/v1/auth/refresh
     * Exchanges a valid refresh token for a new access + refresh pair (token rotation).
     * Old refresh token is atomically invalidated to prevent replay attacks.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * POST /api/v1/auth/oauth2/token
     * Exchanges the short-lived opaque OAuth code for the actual AuthResponse.
     * The code is single-use and expires in 30 seconds.
     * Returns 400 if the code is invalid or expired.
     */
    @PostMapping("/oauth2/token")
    public ResponseEntity<AuthResponse> exchangeOAuthCode(@Valid @RequestBody OAuthCodeRequest request) {
        return ResponseEntity.ok(authService.exchangeOAuthCode(request));
    }

    /**
     * POST /api/v1/auth/logout
     * Revokes all refresh tokens for the authenticated user.
     * The access token remains valid until its natural expiry (max 1 hour).
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }
}
