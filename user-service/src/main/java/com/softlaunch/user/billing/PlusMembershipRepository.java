package com.softlaunch.user.billing;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PlusMembershipRepository extends JpaRepository<PlusMembership, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from PlusMembership m where m.userId = :userId")
    Optional<PlusMembership> findForUpdate(UUID userId);
}
