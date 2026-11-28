package com.softlaunch.user.hearts;

import com.softlaunch.user.billing.PlusEntitlement;
import com.softlaunch.user.billing.PlusMembershipRepository;
import com.softlaunch.user.model.Gender;
import com.softlaunch.user.model.User;
import com.softlaunch.user.repository.ProfileRepository;
import com.softlaunch.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Plus is free for VERIFIED women and non-binary members (the Verified badge stops people faking it).
 * Writes freetier:<id> (calls are free for them; chat-service reads it) and keeps plus:<id> alive (match-service
 * reads it for the Plus perks). Re-checked on login, profile change and verification; 35-day keys, refreshed on login.
 */
@Component
public class FreeTier {

    public static final String KEY = "freetier:";
    private static final Set<Gender> FREE = Set.of(Gender.WOMAN, Gender.FEMALE, Gender.NON_BINARY, Gender.OTHER);
    private static final Duration TTL = Duration.ofDays(35);
    private static final Logger log = LoggerFactory.getLogger(FreeTier.class);

    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PlusMembershipRepository memberships;
    private final PlusEntitlement plus;
    private final StringRedisTemplate redis;

    public FreeTier(UserRepository users, ProfileRepository profiles, PlusMembershipRepository memberships,
                    PlusEntitlement plus, StringRedisTemplate redis) {
        this.users = users;
        this.profiles = profiles;
        this.memberships = memberships;
        this.plus = plus;
        this.redis = redis;
    }

    public boolean eligible(UUID userId) {
        User u = users.findById(userId).orElse(null);
        if (u == null || !u.isVerified()) return false;
        return profiles.findById(userId).map(p -> p.getGender() != null && FREE.contains(p.getGender())).orElse(false);
    }

    /** Re-check and write the Redis flags. Never throws: a Redis hiccup must not break a login. */
    public boolean sync(UUID userId) {
        boolean free = eligible(userId);
        try {
            Instant paidUntil = memberships.findById(userId).map(m -> m.getActiveUntil()).orElse(null);
            if (free) {
                redis.opsForValue().set(KEY + userId, "1", TTL);
                Instant until = Instant.now().plus(TTL);
                plus.activeUntil(userId, paidUntil != null && paidUntil.isAfter(until) ? paidUntil : until);
            } else {
                redis.delete(KEY + userId);
                plus.activeUntil(userId, paidUntil == null ? Instant.EPOCH : paidUntil); // back to what they paid for
            }
        } catch (RuntimeException e) {
            log.debug("free tier sync skipped: {}", e.getClass().getSimpleName());
        }
        return free;
    }
}
