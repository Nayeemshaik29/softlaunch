package com.softlaunch.user.repository;

import com.softlaunch.user.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    /** Only the ids, page by page (the Profile id IS the user id). Used by ai-service's embedding backfill. */
    @org.springframework.data.jpa.repository.Query("select p.userId from Profile p order by p.userId")
    java.util.List<UUID> findAllUserIds(org.springframework.data.domain.Pageable pageable);
}
