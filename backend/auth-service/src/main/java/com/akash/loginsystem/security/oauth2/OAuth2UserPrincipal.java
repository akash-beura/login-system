package com.akash.loginsystem.security.oauth2;

import com.akash.loginsystem.entity.User;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Wraps our internal User entity as an OAuth2User so Spring Security
 * can carry it through the OAuth2 success handler.
 */
@Getter
public class OAuth2UserPrincipal implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;

    public OAuth2UserPrincipal(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<SimpleGrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    /** Spring uses this as the principal name — we use userId for consistency with JWT. */
    @Override
    public String getName() {
        return user.getId().toString();
    }
}
