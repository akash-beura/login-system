package com.akash.loginsystem.service.impl;

import com.akash.loginsystem.config.AppProperties;
import com.akash.loginsystem.dto.request.LoginRequest;
import com.akash.loginsystem.dto.request.OAuthCodeRequest;
import com.akash.loginsystem.dto.request.RefreshRequest;
import com.akash.loginsystem.dto.request.RegisterRequest;
import com.akash.loginsystem.dto.request.SetPasswordRequest;
import com.akash.loginsystem.dto.response.AuthResponse;
import com.akash.loginsystem.dto.response.UserSummaryResponse;
import com.akash.loginsystem.entity.RefreshToken;
import com.akash.loginsystem.entity.User;
import com.akash.loginsystem.exception.InvalidCredentialsException;
import com.akash.loginsystem.exception.OAuthCodeExpiredException;
import com.akash.loginsystem.exception.PasswordAlreadySetException;
import com.akash.loginsystem.exception.PasswordMismatchException;
import com.akash.loginsystem.exception.UserAlreadyExistsException;
import com.akash.loginsystem.model.AuthProvider;
import com.akash.loginsystem.repository.RefreshTokenRepository;
import com.akash.loginsystem.repository.UserRepository;
import com.akash.loginsystem.security.JwtProvider;
import com.akash.loginsystem.security.oauth2.OAuthTokenStore;
import com.akash.loginsystem.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AppProperties appProperties;
    private final OAuthTokenStore oAuthTokenStore;

    // ── Register ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // H-3: Normalize email to lowercase before any DB interaction
        String email = request.getEmail().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(email);
        }

        User user = User.builder()
                .email(email)
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .passwordSet(true)
                .phoneCountryCode(request.getPhoneCountryCode())
                .phoneNumber(request.getPhoneNumber())
                .addressLine1(request.getAddressLine1())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .country(request.getCountry())
                .build();

        userRepository.save(user);
        log.info("Registered new LOCAL user: {}", user.getEmail());

        return issueTokens(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * FIX ISSUE-2: When passwordSet=false, NO token is issued.
     * The user only has a Google credential — they must re-authenticate via Google OAuth
     * to receive a token, then call /set-password to complete account linking.
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // H-3: Normalize email to lowercase
        String email = request.getEmail().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        // Gate: no password has ever been set — this account requires OAuth-first flow.
        // Return the flag with NO tokens; no credential has been verified here.
        if (!user.isPasswordSet()) {
            log.info("Email login blocked for OAuth-only account: {}", user.getEmail());
            return AuthResponse.builder()
                    .requiresPasswordSet(true)
                    .user(UserSummaryResponse.from(user))
                    .build();
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt for email: {}", user.getEmail());
            throw new InvalidCredentialsException();
        }

        log.info("Successful login for: {}", user.getEmail());
        return issueTokens(user);
    }

    // ── Set Password ──────────────────────────────────────────────────────────

    /**
     * C-1 FIX: Account-linking flow only — blocked if password is already set.
     * A LOCAL user or a previously linked OAuth user cannot use this endpoint to
     * silently override their password without old-password verification.
     */
    @Override
    @Transactional
    public AuthResponse setPassword(UUID userId, SetPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        // Guard: if password is already set this endpoint must not be used
        if (user.isPasswordSet()) {
            log.warn("setPassword called on account that already has a password: {}", user.getEmail());
            throw new PasswordAlreadySetException();
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPasswordSet(true);
        userRepository.save(user);

        log.info("Password set for user: {}", user.getEmail());
        return issueTokens(user);
    }

    // ── OAuth2 Login ──────────────────────────────────────────────────────────

    /**
     * FIX ISSUE-1 (part): Issues full token pair for a Google-authenticated user.
     * Called only from OAuth2SuccessHandler — identity already verified by Google.
     */
    @Override
    @Transactional
    public AuthResponse completeOAuthLogin(User user) {
        log.info("Completing OAuth login for: {}", user.getEmail());
        return issueTokens(user);
    }

    // ── OAuth Code Exchange ───────────────────────────────────────────────────

    /**
     * M-1 FIX: Moved from AuthController — the service layer owns the token store interaction.
     * Consumes the short-lived opaque code and returns the stored AuthResponse.
     */
    @Override
    public AuthResponse exchangeOAuthCode(OAuthCodeRequest request) {
        return oAuthTokenStore.consume(request.getCode())
                .orElseThrow(OAuthCodeExpiredException::new);
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * C-2 FIX: Invalidates all refresh tokens for the user.
     * The access token remains valid until its natural expiry (max 1 hour).
     * M-5 NOTE: This system enforces a single active session per user — issueTokens()
     * always calls deleteByUser() before issuing a new token, ensuring only one refresh
     * token exists per account at any time. logout() follows the same convention.
     */
    @Override
    @Transactional
    public void logout(UUID userId) {
        User user = userRepository.getReferenceById(userId);
        refreshTokenRepository.deleteByUser(user);
        log.info("User logged out, refresh tokens revoked: {}", userId);
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    /**
     * C-3 FIX: Atomic token rotation using deleteByTokenValue() instead of a non-atomic
     * find-then-delete pattern. If the delete returns 0 rows, the token was already
     * consumed by a concurrent request — prevents refresh token replay.
     */
    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        // Fetch before deletion to read expiry and user association
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(InvalidCredentialsException::new);

        if (Instant.now().isAfter(stored.getExpiresAt())) {
            // Delete the expired token to prevent orphaned rows, then fail
            refreshTokenRepository.deleteByTokenValue(request.getRefreshToken());
            log.warn("Expired refresh token used for user: {}", stored.getUser().getEmail());
            throw new InvalidCredentialsException();
        }

        // Atomically consume the token — if 0 rows deleted, a concurrent request already used it
        int deleted = refreshTokenRepository.deleteByTokenValue(request.getRefreshToken());
        if (deleted == 0) {
            log.warn("Refresh token replay attempt detected for user: {}", stored.getUser().getEmail());
            throw new InvalidCredentialsException();
        }

        User user = stored.getUser();
        log.info("Refresh token rotated for: {}", user.getEmail());
        return issueTokens(user);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Issues a new access + refresh token pair.
     * M-5: Calls deleteByUser() first — enforces the single active session policy.
     * Any existing refresh token (from another device or prior login) is invalidated.
     */
    private AuthResponse issueTokens(User user) {
        // Single-session enforcement: remove any pre-existing refresh token before issuing a new one
        refreshTokenRepository.deleteByUser(user);

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshTokenValue = jwtProvider.generateRefreshTokenValue();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiresAt(Instant.now().plusMillis(appProperties.getJwt().getRefreshExpiryMs()))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .requiresPasswordSet(!user.isPasswordSet())
                .user(UserSummaryResponse.from(user))
                .build();
    }
}
