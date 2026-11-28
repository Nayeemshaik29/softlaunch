package com.softlaunch.user.hearts;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HeartWalletRepository extends JpaRepository<HeartWallet, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from HeartWallet w where w.userId = :id")
    Optional<HeartWallet> findForUpdate(UUID id);
}
