package com.softlaunch.chat.call;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Who may call, and what it costs (on top of both people's consent, which CallService checks first):
 *  - The first 24 hours after a match's first answered call are free for both.
 *  - After that: verified women / non-binary members call free (freetier:<id>); everyone else needs today's
 *    ₹50 / 10 ❤ pass for that match (callpass:<match>:<id>, written by user-service, ends at midnight IST).
 *    BOTH people need access ("each person pays").
 *  - Video unlocks after calls on 3 different days. The first day video is used afterwards is free for both
 *    ("day 4: video's on us"); later video needs access like audio.
 * Redis: call:first:<match> (epoch ms) · call:days:<match> (set of IST dates) · call:freevideo:<match> (IST date)
 */
@Component
public class CallRules {

    static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    static final Duration FREE_WINDOW = Duration.ofHours(24);
    static final int DAYS_FOR_VIDEO = 3;
    private static final Duration KEEP = Duration.ofDays(120);

    public enum Access { FREE_TIER, FREE_WINDOW, PASS, NONE }

    public record Status(CallDtos.Consent consent, Instant freeUntil, boolean firstCallDone, int callDays, int daysForVideo,
                         boolean videoUnlocked, boolean freeVideoToday, Access me, Access them, Instant passEndsAt) {
    }

    private final StringRedisTemplate redis;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public CallRules(StringRedisTemplate redis) {
        this(redis, Clock.systemUTC());
    }

    CallRules(StringRedisTemplate redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    /** null = allowed; otherwise the reason code the app shows. */
    public String problem(UUID me, UUID other, UUID matchId, boolean video) {
        if (!video) {
            if (access(me, matchId) == Access.NONE) return "pass_needed";
            if (access(other, matchId) == Access.NONE) return "their_pass_needed";
            return null;
        }
        if (callDays(matchId) < DAYS_FOR_VIDEO) return "video_locked";
        if (freeVideoToday(matchId)) return null;
        if (paidOrFree(me, matchId) == Access.NONE) return "pass_needed";
        if (paidOrFree(other, matchId) == Access.NONE) return "their_pass_needed";
        return null;
    }

    /** A call was answered (or switched to video): start the free window, count the day, use up the free video day. */
    public void callAnswered(UUID matchId, boolean video) {
        String first = "call:first:" + matchId;
        redis.opsForValue().setIfAbsent(first, Long.toString(clock.millis()), KEEP);
        String days = "call:days:" + matchId;
        redis.opsForSet().add(days, today().toString());
        redis.expire(days, KEEP);
        if (video && callDays(matchId) >= DAYS_FOR_VIDEO) {
            redis.opsForValue().setIfAbsent("call:freevideo:" + matchId, today().toString(), KEEP);
        }
    }

    public Status status(UUID me, UUID other, UUID matchId, CallDtos.Consent consent) {
        Instant first = firstCall(matchId);
        int days = callDays(matchId);
        Instant passEnds = today().plusDays(1).atStartOfDay(IST).toInstant();
        return new Status(consent, first == null ? null : first.plus(FREE_WINDOW), first != null, days, DAYS_FOR_VIDEO,
                days >= DAYS_FOR_VIDEO, days >= DAYS_FOR_VIDEO && freeVideoToday(matchId),
                access(me, matchId), access(other, matchId), passEnds);
    }

    public void forget(UUID matchId) {
        redis.delete(List.of("call:first:" + matchId, "call:days:" + matchId, "call:freevideo:" + matchId));
    }

    // ------------------------------------------------------------------ helpers

    Access access(UUID user, UUID matchId) {
        Access paid = paidOrFree(user, matchId);
        if (paid != Access.NONE) return paid;
        Instant first = firstCall(matchId);
        return first == null || clock.instant().isBefore(first.plus(FREE_WINDOW)) ? Access.FREE_WINDOW : Access.NONE;
    }

    private Access paidOrFree(UUID user, UUID matchId) {
        if (Boolean.TRUE.equals(redis.hasKey("freetier:" + user))) return Access.FREE_TIER;
        if (Boolean.TRUE.equals(redis.hasKey("callpass:" + matchId + ":" + user))) return Access.PASS;
        return Access.NONE;
    }

    private boolean freeVideoToday(UUID matchId) {
        String day = redis.opsForValue().get("call:freevideo:" + matchId);
        return day == null || day.equals(today().toString());   // not used yet, or it's that day
    }

    private Instant firstCall(UUID matchId) {
        String v = redis.opsForValue().get("call:first:" + matchId);
        return v == null ? null : Instant.ofEpochMilli(Long.parseLong(v));
    }

    private int callDays(UUID matchId) {
        Long n = redis.opsForSet().size("call:days:" + matchId);
        return n == null ? 0 : n.intValue();
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(IST));
    }
}
