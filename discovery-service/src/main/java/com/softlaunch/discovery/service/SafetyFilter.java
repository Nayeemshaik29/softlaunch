package com.softlaunch.discovery.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Who must NOT appear in a user's feed, kept in Redis from safety events:
 *   blocking:<A>   people A blocked
 *   blockedby:<B>  people who blocked B
 *   under_review   users moderators must check first (hidden from everyone)
 * Two directional sets, so "A blocked B" and "B blocked A" can be undone independently.
 */
@Component
public class SafetyFilter {

    private static final String UNDER_REVIEW = "under_review";

    private final StringRedisTemplate redis;

    public SafetyFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void block(UUID blocker, UUID blocked) {
        redis.opsForSet().add("blocking:" + blocker, blocked.toString());
        redis.opsForSet().add("blockedby:" + blocked, blocker.toString());
    }

    public void unblock(UUID blocker, UUID blocked) {
        redis.opsForSet().remove("blocking:" + blocker, blocked.toString());
        redis.opsForSet().remove("blockedby:" + blocked, blocker.toString());
    }

    public void setUnderReview(UUID userId, boolean underReview) {
        if (underReview) {
            redis.opsForSet().add(UNDER_REVIEW, userId.toString());
        } else {
            redis.opsForSet().remove(UNDER_REVIEW, userId.toString());
        }
    }

    /** Everyone hidden from me: people I blocked, people who blocked me, and everyone under review. */
    public Set<String> hiddenFrom(UUID me) {
        Set<String> hidden = new HashSet<>();
        Set<String> union = redis.opsForSet().union("blocking:" + me, Set.of("blockedby:" + me, UNDER_REVIEW));
        if (union != null) {
            hidden.addAll(union);
        }
        return hidden;
    }
}
