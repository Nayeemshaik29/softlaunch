package com.softlaunch.user.repository;

import com.softlaunch.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByGoogleSub(String googleSub);

    long countByCreatedAtAfter(java.time.Instant from);

    long countByCreatedAtBetween(java.time.Instant from, java.time.Instant to);

    java.util.List<User> findByDisplayNameContainingIgnoreCase(String name, org.springframework.data.domain.Pageable pageable);
}