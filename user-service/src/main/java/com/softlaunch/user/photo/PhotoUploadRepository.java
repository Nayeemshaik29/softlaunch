package com.softlaunch.user.photo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoUploadRepository extends JpaRepository<PhotoUpload, UUID> {

    Optional<PhotoUpload> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndStatusAndExpiresAtAfter(UUID userId, PhotoUpload.Status status, Instant now);

    long deleteByUserId(UUID userId);   // account deletion (runs inside its transaction)

    List<PhotoUpload> findTop200ByStatusAndExpiresAtBefore(PhotoUpload.Status status, Instant now);
}
