package com.softlaunch.match.repository;

import com.softlaunch.match.model.Swipe;
import com.softlaunch.match.model.SwipeDirection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface SwipeRepository extends JpaRepository<Swipe, UUID> {

    boolean existsBySwiperIdAndTargetId(UUID swiperId, UUID targetId);

    boolean existsBySwiperIdAndTargetIdAndDirectionIn(UUID swiperId, UUID targetId, Collection<SwipeDirection> directions);
}