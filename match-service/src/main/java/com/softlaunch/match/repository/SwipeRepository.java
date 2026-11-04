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

    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from Swipe s where (s.swiperId = :a and s.targetId = :b) or (s.swiperId = :b and s.targetId = :a)")
    int deleteBetween(@org.springframework.data.repository.query.Param("a") UUID a,
                      @org.springframework.data.repository.query.Param("b") UUID b);

    @Query("select s.targetId from Swipe s where s.swiperId = :swiperId")
    List<UUID> findTargetIdsBySwiperId(@Param("swiperId") UUID swiperId);
}