package com.akash.loginsystem.security.oauth2;

import com.akash.loginsystem.entity.User;
import com.akash.loginsystem.model.AuthProvider;
import com.akash.loginsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Maps the Google OAuth2User to our internal User entity.
 *
 * Account-linking logic:
 * - If email exists (registered via LOCAL) → link: update providerId, keep passwordSet as-is.
 * - If new Google user → create with password=null, passwordSet=false.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // S-4: Guard against Google returning null claims (unverified or restricted accounts)
        String rawEmail = oAuth2User.getAttribute("email");
        String name     = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub");

        // H-3: Normalize email to lowercase for consistent account matching across providers
        String email = (rawEmail != null) ? rawEmail.toLowerCase(Locale.ROOT) : null;

        if (email == null || providerId == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "Google did not return required user attributes (email, sub)");
        }

        // Use email as fallback display name if Google omits it
        String resolvedName = (name != null) ? name : email;

        User user = userRepository.findByEmail(email)
                .map(existing -> linkGoogleAccount(existing, providerId))
                .orElseGet(() -> createGoogleUser(email, resolvedName, providerId));

        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Existing user (LOCAL or previous OAuth) — attach Google sub if missing. */
    private User linkGoogleAccount(User user, String providerId) {
        if (user.getProviderId() == null) {
            user.setProviderId(providerId);
            // Do NOT overwrite provider — preserve LOCAL so password login still works.
            log.info("Linked Google account to existing user: {}", user.getEmail());
            userRepository.save(user);
        }
        return user;
    }

    /** Brand-new user arriving via Google — password is null, passwordSet=false. */
    private User createGoogleUser(String email, String name, String providerId) {
        User newUser = User.builder()
                .email(email)
                .name(name)
                .provider(AuthProvider.GOOGLE)
                .providerId(providerId)
                .password(null)
                .passwordSet(false)
                .build();
        log.info("Creating new Google user: {}", email);
        return userRepository.save(newUser);
    }
}
