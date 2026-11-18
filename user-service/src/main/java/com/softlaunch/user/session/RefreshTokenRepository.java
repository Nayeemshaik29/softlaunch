package com.softlaunch.user.session;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /** Row lock: two requests with the same token can't both rotate it. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.familyId = :family and t.revokedAt is null")
    int revokeFamily(UUID family, Instant now);

    @Modifying
    @Query("delete from RefreshToken t where t.userId = :userId")
    int deleteAllOfUser(UUID userId);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(Instant cutoff);
}
