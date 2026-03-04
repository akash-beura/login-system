package com.akash.loginsystem.controller;

import com.akash.loginsystem.dto.request.LoginRequest;
import com.akash.loginsystem.dto.request.OAuthCodeRequest;
import com.akash.loginsystem.dto.request.RefreshRequest;
import com.akash.loginsystem.dto.request.RegisterRequest;
import com.akash.loginsystem.dto.request.SetPasswordRequest;
import com.akash.loginsystem.dto.response.AuthResponse;
import com.akash.loginsystem.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
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
     * refreshToken is set as HttpOnly cookie, NOT in response body.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request);
        setRefreshTokenCookie(response, authResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    /**
     * POST /api/v1/auth/login
     * Authenticates by email + password.
     *
     * If passwordSet=false (OAuth-only account), returns:
     *   { requiresPasswordSet: true, user: {...} }   ← NO tokens
     * Client must re-authenticate via Google OAuth to obtain a token,
     * then call /set-password to complete account linking.
     *
     * On success, refreshToken is set as HttpOnly cookie, NOT in response body.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        if (authResponse.getAccessToken() != null) {
            setRefreshTokenCookie(response, authResponse);
        }
        return ResponseEntity.ok(authResponse);
    }

    /**
     * POST /api/v1/auth/set-password
     * Account-linking flow — first-time password set for OAuth users (passwordSet=false).
     * Blocked with 409 if the account already has a password set.
     * Requires Bearer token obtained via Google OAuth redirect + /oauth2/token exchange.
     *
     * On success, refreshToken is set as HttpOnly cookie, NOT in response body.
     */
    @PostMapping("/set-password")
    public ResponseEntity<AuthResponse> setPassword(
            @Valid @RequestBody SetPasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletResponse response) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        AuthResponse authResponse = authService.setPassword(userId, request);
        setRefreshTokenCookie(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * POST /api/v1/auth/refresh
     * Exchanges a valid refresh token for a new access + refresh pair (token rotation).
     * Old refresh token is atomically invalidated to prevent replay attacks.
     *
     * refreshToken is read from HttpOnly cookie (no request body needed).
     * New refreshToken is set as HttpOnly cookie in response, NOT in response body.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response) {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);
        AuthResponse authResponse = authService.refresh(request);
        setRefreshTokenCookie(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * POST /api/v1/auth/oauth2/token
     * Exchanges the short-lived opaque OAuth code for the actual AuthResponse.
     * The code is single-use and expires in 30 seconds.
     * Returns 400 if the code is invalid or expired.
     *
     * refreshToken is set as HttpOnly cookie, NOT in response body.
     */
    @PostMapping("/oauth2/token")
    public ResponseEntity<AuthResponse> exchangeOAuthCode(
            @Valid @RequestBody OAuthCodeRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.exchangeOAuthCode(request);
        setRefreshTokenCookie(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * POST /api/v1/auth/logout
     * Revokes all refresh tokens for the authenticated user.
     * The access token remains valid until its natural expiry (max 1 hour).
     * Clears the refreshToken cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletResponse response) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        authService.logout(userId);
        clearRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }

    /**
     * Sets the refreshToken as an HttpOnly, Secure, SameSite=Strict cookie.
     * The cookie is only sent to /api/v1/auth/refresh endpoint.
     */
    private void setRefreshTokenCookie(HttpServletResponse response, AuthResponse authResponse) {
        // AuthResponse still contains the refreshToken internally for service use
        // but it won't be serialized in the JSON response body due to AuthResponse DTO changes
        String refreshToken = authResponse.getRefreshToken();
        if (refreshToken == null) {
            return;
        }

        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true); // only sent over HTTPS
        refreshTokenCookie.setPath("/api/v1/auth/refresh"); // only sent to this endpoint
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(refreshTokenCookie);

        // Add SameSite attribute via Set-Cookie header for CSRF protection
        String setCookieHeader = response.getHeader("Set-Cookie");
        if (setCookieHeader != null && !setCookieHeader.isEmpty()) {
            response.setHeader("Set-Cookie", setCookieHeader + "; SameSite=Strict");
        }
    }

    /**
     * Clears the refreshToken cookie by setting it to empty with MaxAge=0.
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie clearCookie = new Cookie("refreshToken", "");
        clearCookie.setMaxAge(0);
        clearCookie.setPath("/api/v1/auth/refresh");
        clearCookie.setHttpOnly(true);
        clearCookie.setSecure(true);
        response.addCookie(clearCookie);
    }
}
