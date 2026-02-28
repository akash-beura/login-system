package com.akash.loginsystem.service.impl;

import com.akash.loginsystem.config.AppProperties;
import com.akash.loginsystem.dto.request.LoginRequest;
import com.akash.loginsystem.dto.request.RefreshRequest;
import com.akash.loginsystem.dto.request.RegisterRequest;
import com.akash.loginsystem.dto.request.SetPasswordRequest;
import com.akash.loginsystem.dto.response.AuthResponse;
import com.akash.loginsystem.dto.response.UserResponse;
import com.akash.loginsystem.entity.RefreshToken;
import com.akash.loginsystem.entity.User;
import com.akash.loginsystem.exception.InvalidCredentialsException;
import com.akash.loginsystem.exception.PasswordMismatchException;
import com.akash.loginsystem.exception.UserAlreadyExistsException;
import com.akash.loginsystem.model.AuthProvider;
import com.akash.loginsystem.repository.RefreshTokenRepository;
import com.akash.loginsystem.repository.UserRepository;
import com.akash.loginsystem.security.JwtProvider;
import com.akash.loginsystem.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    // ── Register ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .passwordSet(true)
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
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        // Gate: no password has ever been set — this account requires OAuth-first flow.
        // Return the flag with NO tokens; no credential has been verified here.
        if (!user.isPasswordSet()) {
            log.info("Email login blocked for OAuth-only account: {}", user.getEmail());
            return AuthResponse.builder()
                    .requiresPasswordSet(true)
                    .user(UserResponse.from(user))
                    .build();
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new InvalidCredentialsException();
        }

        log.info("Successful login for: {}", user.getEmail());
        return issueTokens(user);
    }

    // ── Set Password ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse setPassword(UUID userId, SetPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

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

    // ── Refresh ───────────────────────────────────────────────────────────────

    /**
     * FIX ISSUE-7: Validates and rotates the refresh token.
     * Expired tokens are deleted before throwing to prevent orphaned rows.
     */
    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(InvalidCredentialsException::new);

        if (Instant.now().isAfter(stored.getExpiresAt())) {
            refreshTokenRepository.delete(stored);
            log.warn("Expired refresh token used for user: {}", stored.getUser().getEmail());
            throw new InvalidCredentialsException();
        }

        User user = stored.getUser();
        // Delete the old token before rotation — issueTokens will deleteByUser then save new
        log.info("Refresh token rotated for: {}", user.getEmail());
        return issueTokens(user);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private AuthResponse issueTokens(User user) {
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
                .requiresPasswordSet(false)
                .user(UserResponse.from(user))
                .build();
    }
}
