package com.softlaunch.discovery.service;

import com.softlaunch.discovery.dto.FeedCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * King of the Hill: people who wear a crown (user-service writes them to the "crowns" sorted set, score = expiry)
 * go to the top of everyone's deck that already contains them (location and preferences still decide WHO you see).
 * Applied on every read, so a 30-minute crown shows up and disappears on time even with cached feeds.
 */
@Component
public class CrownBoost {

    private static final Logger log = LoggerFactory.getLogger(CrownBoost.class);
    private final StringRedisTemplate redis;

    public CrownBoost(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public List<FeedCard> apply(List<FeedCard> feed) {
        if (feed.isEmpty()) return feed;
        Map<String, Instant> crowned = new HashMap<>();
        try {
            long now = System.currentTimeMillis();
            redis.opsForZSet().removeRangeByScore("crowns", 0, now);   // expired crowns
            Set<ZSetOperations.TypedTuple<String>> live = redis.opsForZSet().rangeByScoreWithScores("crowns", now, Double.MAX_VALUE);
            if (live != null) live.forEach(t -> crowned.put(t.getValue(), Instant.ofEpochMilli(t.getScore().longValue())));
        } catch (RuntimeException e) {
            log.debug("crowns skipped: {}", e.getClass().getSimpleName());
            return feed;
        }
        if (crowned.isEmpty()) return feed;
        List<FeedCard> top = new ArrayList<>();
        List<FeedCard> rest = new ArrayList<>();
        for (FeedCard c : feed) {
            Instant until = crowned.get(c.userId().toString());
            if (until != null) top.add(c.crowned(until));
            else rest.add(c);
        }
        top.sort(Comparator.comparing(FeedCard::crownedUntil).reversed());   // newest crown first
        top.addAll(rest);
        return top;
    }
}
