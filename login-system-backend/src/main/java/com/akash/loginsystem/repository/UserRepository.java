package com.akash.loginsystem.repository;

import com.akash.loginsystem.entity.User;
import com.akash.loginsystem.model.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderIdAndProvider(String providerId, AuthProvider provider);

    boolean existsByEmail(String email);
}
