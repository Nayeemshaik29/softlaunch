package com.softlaunch.verification.repository;

import com.softlaunch.verification.model.Verification;
import com.softlaunch.verification.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationRepository extends JpaRepository<Verification, UUID> {
    boolean existsByUserIdAndStatus(UUID userId, VerificationStatus status);
    boolean existsBySubjectHashAndStatus(String subjectHash, VerificationStatus status);
    Optional<Verification> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
