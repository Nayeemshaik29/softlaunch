package com.softlaunch.match.repository;

import com.softlaunch.match.model.Swipe;
import com.softlaunch.match.model.SwipeDirection;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SwipeRepository extends JpaRepository<Swipe, UUID> {

    boolean existsBySwiperIdAndTargetId(UUID swiperId, UUID targetId);

    boolean existsBySwiperIdAndTargetIdAndDirectionIn(UUID swiperId, UUID targetId, Collection<SwipeDirection> directions);

    @Query("select s.targetId from Swipe s where s.swiperId = :swiperId")
    List<UUID> findTargetIdsBySwiperId(@Param("swiperId") UUID swiperId);
}