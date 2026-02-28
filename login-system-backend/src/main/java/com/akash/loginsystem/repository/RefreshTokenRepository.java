package com.akash.loginsystem.repository;

import com.akash.loginsystem.entity.RefreshToken;
import com.akash.loginsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    /** Remove all tokens for a user (logout / token rotation). */
    @Modifying
    @Transactional
    void deleteByUser(User user);

    /** Cleanup job hook — remove expired tokens. Needs own transaction for scheduler context. */
    @Modifying
    @Transactional
    void deleteByExpiresAtBefore(Instant threshold);
}
