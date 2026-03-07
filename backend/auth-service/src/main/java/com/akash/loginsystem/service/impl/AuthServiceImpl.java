package com.akash.loginsystem.service.impl;

import com.akash.loginsystem.dto.request.LoginRequest;
import com.akash.loginsystem.dto.request.OAuthCodeRequest;
import com.akash.loginsystem.dto.request.RegisterRequest;
import com.akash.loginsystem.dto.request.SetPasswordRequest;
import com.akash.loginsystem.dto.response.AuthResponse;
import com.akash.loginsystem.dto.response.UserSummaryResponse;
import com.akash.loginsystem.entity.User;
import com.akash.loginsystem.exception.InvalidCredentialsException;
import com.akash.loginsystem.exception.OAuthCodeExpiredException;
import com.akash.loginsystem.exception.PasswordAlreadySetException;
import com.akash.loginsystem.exception.PasswordMismatchException;
import com.akash.loginsystem.exception.UserAlreadyExistsException;
import com.akash.loginsystem.model.AuthProvider;
import com.akash.loginsystem.repository.UserRepository;
import com.akash.loginsystem.security.JwtProvider;
import com.akash.loginsystem.security.oauth2.OAuthTokenStore;
import com.akash.loginsystem.service.AuthService;
import com.akash.loginsystem.service.SessionTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final SessionTokenService sessionTokenService;
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

    @Override
    public void logout(UUID userId, String sessionToken) {
        sessionTokenService.invalidate(sessionToken);
        log.info("User logged out, session invalidated: {}", userId);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /** Issues a JWT access token + Redis session token. */
    private AuthResponse issueTokens(User user) {
        String accessToken = jwtProvider.generateAccessToken(user);
        String sessionToken = sessionTokenService.createSession(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .sessionToken(sessionToken)
                .requiresPasswordSet(!user.isPasswordSet())
                .user(UserSummaryResponse.from(user))
                .build();
    }
}
