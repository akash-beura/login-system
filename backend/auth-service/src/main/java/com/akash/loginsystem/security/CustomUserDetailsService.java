package com.akash.loginsystem.security;

import com.akash.loginsystem.entity.User;
import com.akash.loginsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Loads UserDetails by email (for form login) or by UUID (for JWT filter).
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /** Used by Spring Security's form-login / DaoAuthenticationProvider. */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email: " + email));
        return toUserDetails(user);
    }

    /** Used by JwtAuthFilter — looks up by the `sub` claim (userId). */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UsernameNotFoundException("No user with id: " + userId));
        return toUserDetails(user);
    }

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getId().toString())
                .password(user.getPassword() != null ? user.getPassword() : "")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }
}
