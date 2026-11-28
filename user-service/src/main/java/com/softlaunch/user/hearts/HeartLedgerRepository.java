package com.softlaunch.user.hearts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface HeartLedgerRepository extends JpaRepository<HeartLedgerEntry, UUID> {

    List<HeartLedgerEntry> findTop20ByUserIdOrderByAtDesc(UUID userId);

    @Modifying
    @Query("delete from HeartLedgerEntry e where e.userId = :userId")
    int deleteAllOf(UUID userId);
}
