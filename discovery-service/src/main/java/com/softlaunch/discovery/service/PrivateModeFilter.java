package com.softlaunch.discovery.service;

import com.softlaunch.discovery.client.SwipedUsersClient;
import com.softlaunch.discovery.dto.FeedCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Private Mode: someone in it is shown ONLY to people they have liked.
 * The set of private users lives in Redis (written by user-service) and is checked on every feed read, so
 * switching Private Mode on hides you from feeds that were already cached.
 * Fails closed: if we can't tell, private users are hidden.
 */
@Component
public class PrivateModeFilter {

    static final String KEY = "privacy:private_mode";   // same keys as user-service PrivateModeRegistry
    static final String VEILED_KEY = "privacy:veiled";
    private static final Logger log = LoggerFactory.getLogger(PrivateModeFilter.class);

    private final StringRedisTemplate redis;
    private final SwipedUsersClient swipes;

    public PrivateModeFilter(StringRedisTemplate redis, SwipedUsersClient swipes) {
        this.redis = redis;
        this.swipes = swipes;
    }

    public List<FeedCard> apply(UUID me, List<FeedCard> feed) {
        return hidePrivate(me, stripVeiled(feed));
    }

    /**
     * Someone veiled their photos AFTER this feed was cached: the card still has real links.
     * Remove them now (the app shows the blur placeholder); the next rebuild brings the blurred copies.
     */
    private List<FeedCard> stripVeiled(List<FeedCard> feed) {
        List<FeedCard> stale = feed.stream().filter(c -> !c.veiled() && !c.photoUrls().isEmpty()).toList();
        if (stale.isEmpty()) return feed;
        Map<Object, Boolean> veiled;
        try {
            veiled = redis.opsForSet().isMember(VEILED_KEY, stale.stream().map(c -> c.userId().toString()).toArray());
        } catch (DataAccessException e) {
            return feed;
        }
        if (veiled == null || !veiled.containsValue(Boolean.TRUE)) return feed;
        return feed.stream().map(c -> Boolean.TRUE.equals(veiled.get(c.userId().toString())) && !c.veiled()
                ? new FeedCard(c.userId(), c.displayName(), c.verified(), c.age(), c.distanceKm(), c.bio(), c.city(),
                        c.lookingFor(), c.sharedInterests(), List.of(), c.reasons(), c.score(), true, c.mutualDesires())
                : c).toList();
    }

    private List<FeedCard> hidePrivate(UUID me, List<FeedCard> feed) {
        if (feed.isEmpty()) return feed;
        Object[] ids = feed.stream().map(c -> c.userId().toString()).toArray();
        Map<Object, Boolean> isPrivate;
        try {
            isPrivate = redis.opsForSet().isMember(KEY, ids);   // one round trip (SMISMEMBER)
        } catch (DataAccessException e) {
            log.warn("Private Mode check unavailable ({}); hiding nobody extra", e.getClass().getSimpleName());
            return feed;   // Redis down: feeds already depend on Redis GEO, so this path is effectively unreachable
        }
        if (isPrivate == null || !isPrivate.containsValue(Boolean.TRUE)) return feed;
        Set<String> likedMe = swipes.likedBy(me);   // only fetched when there IS a private card
        return feed.stream()
                .filter(c -> !Boolean.TRUE.equals(isPrivate.get(c.userId().toString())) || likedMe.contains(c.userId().toString()))
                .toList();
    }
}
