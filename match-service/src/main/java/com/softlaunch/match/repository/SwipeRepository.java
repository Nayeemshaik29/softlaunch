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

    /** Account deletion: their swipes AND everyone's swipes on them. */
    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from Swipe s where s.swiperId = :user or s.targetId = :user")
    int deleteAllInvolving(@Param("user") UUID user);

    @Query("select s.targetId from Swipe s where s.swiperId = :swiperId")
    List<UUID> findTargetIdsBySwiperId(@Param("swiperId") UUID swiperId);

    java.util.Optional<Swipe> findFirstBySwiperIdOrderByCreatedAtDesc(UUID swiperId);

    /** People who liked me and whom I haven't answered yet, minus blocks either way. Newest first. */
    @Query("select s from Swipe s where s.targetId = :me"
            + " and s.direction in (com.softlaunch.match.model.SwipeDirection.LIKE, com.softlaunch.match.model.SwipeDirection.SUPER_LIKE)"
            + " and not exists (select 1 from Swipe m where m.swiperId = :me and m.targetId = s.swiperId)"
            + " and not exists (select 1 from UserBlock b where (b.blockerId = :me and b.blockedId = s.swiperId)"
            + "                 or (b.blockerId = s.swiperId and b.blockedId = :me))"
            + " order by s.createdAt desc")
    List<Swipe> findPendingLikesOf(@Param("me") UUID me);

    /** Who liked (or super liked) this user: a person in Private Mode is only shown to these people. */
    @Query("select s.swiperId from Swipe s where s.targetId = :targetId and s.direction in "
            + "(com.softlaunch.match.model.SwipeDirection.LIKE, com.softlaunch.match.model.SwipeDirection.SUPER_LIKE)")
    List<UUID> findLikerIdsOf(@Param("targetId") UUID targetId);
}