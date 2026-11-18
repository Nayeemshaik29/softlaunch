package com.softlaunch.match.repository;

import com.softlaunch.match.model.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    @Modifying
    @Query("delete from UserBlock b where b.blockerId = :blocker and b.blockedId = :blocked")
    int deleteDirection(@Param("blocker") UUID blocker, @Param("blocked") UUID blocked);

    /** Account deletion: blocks they made and blocks against them (the person no longer exists). */
    @Modifying
    @Query("delete from UserBlock b where b.blockerId = :user or b.blockedId = :user")
    int deleteAllInvolving(@Param("user") UUID user);

    /** Blocked in EITHER direction. */
    default boolean isBlocked(UUID a, UUID b) {
        return existsByBlockerIdAndBlockedId(a, b) || existsByBlockerIdAndBlockedId(b, a);
    }
}
