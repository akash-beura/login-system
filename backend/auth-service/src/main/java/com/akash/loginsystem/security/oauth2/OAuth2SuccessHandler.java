package com.akash.loginsystem.security.oauth2;

import com.akash.loginsystem.config.AppProperties;
import com.akash.loginsystem.dto.response.AuthResponse;
import com.akash.loginsystem.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Called after a successful Google OAuth2 login.
 *
 * FIX ISSUE-1: JWT is no longer placed in the redirect URL (CWE-598).
 * Instead, the full AuthResponse is stored in OAuthTokenStore under an opaque
 * one-time code (30 s TTL). The redirect URL carries only that code.
 * The React frontend exchanges the code for tokens via:
 *   POST /api/v1/auth/oauth2/token  { "code": "<opaque>" }
 *
 * If the user has no password yet (passwordSet=false), redirect target is
 * /set-password so they complete account linking after exchanging the code.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final OAuthTokenStore oAuthTokenStore;
    private final AppProperties appProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();

        AuthResponse authResponse = authService.completeOAuthLogin(principal.getUser());

        // Store tokens; redirect carries only the opaque code
        String code = oAuthTokenStore.store(authResponse);

        // Always redirect to the unified frontend callback page.
        // OAuthCallbackPage exchanges the code, reads requiresPasswordSet from
        // the AuthResponse, and navigates to /set-password or /homepage internally.
        String redirectUrl = UriComponentsBuilder
                .fromUriString(appProperties.getFrontendUrl() + "/oauth/callback")
                .queryParam("code", code)
                .build()
                .toUriString();

        log.info("OAuth2 login success for {}. Redirecting to /oauth/callback with one-time code.",
                principal.getUser().getEmail());

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
