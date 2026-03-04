package com.akash.loginsystem.repository;

import com.akash.loginsystem.entity.RefreshToken;
import com.akash.loginsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Atomically deletes a single refresh token by its string value.
     * Returns the number of rows deleted (1 = success, 0 = already consumed or not found).
     * Used for race-safe token rotation — prevents concurrent refresh replays.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken r WHERE r.token = :token")
    int deleteByTokenValue(@Param("token") String token);

    /** Cleanup job hook — remove expired tokens. Needs own transaction for scheduler context. */
    @Modifying
    @Transactional
    void deleteByExpiresAtBefore(Instant threshold);
}
