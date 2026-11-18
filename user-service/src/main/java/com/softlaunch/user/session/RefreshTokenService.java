package com.softlaunch.user.session;

import com.softlaunch.user.model.User;
import com.softlaunch.user.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Refresh tokens keep people logged in for weeks while access tokens (JWT) stay short (15 min).
 *
 *  - Rotation: every refresh returns a NEW refresh token and retires the old one.
 *  - Reuse detection: a retired token coming back means someone copied it (or a bug). We can't tell
 *    the thief from the owner, so the whole session (family) is revoked and both must log in again.
 *  - Only SHA-256 hashes are stored.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository tokens;
    private final UserRepository users;
    private final MeterRegistry metrics;
    private final Duration ttl;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired   // the other constructor is for tests (fixed clock)
    public RefreshTokenService(RefreshTokenRepository tokens, UserRepository users, MeterRegistry metrics,
                               @Value("${softlaunch.auth.refresh-ttl:30d}") Duration ttl) {
        this(tokens, users, metrics, ttl, Clock.systemUTC());
    }

    RefreshTokenService(RefreshTokenRepository tokens, UserRepository users, MeterRegistry metrics, Duration ttl, Clock clock) {
        this.tokens = tokens;
        this.users = users;
        this.metrics = metrics;
        this.ttl = ttl;
        this.clock = clock;
    }

    public Duration ttl() {
        return ttl;
    }

    public record Rotation(User user, String refreshToken) {
    }

    /** New login = new session family. Returns the raw token (only ever sent to the client, never stored). */
    @Transactional
    public String startSession(UUID userId) {
        return issue(userId, UUID.randomUUID());
    }

    /** Swap a valid refresh token for a new one (same session). */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class) // a reuse revocation must be saved
    public Rotation rotate(String raw) {
        Instant now = clock.instant();
        RefreshToken current = tokens.findByTokenHash(hash(raw)).orElseThrow(() -> {
            count("unknown");
            return new InvalidRefreshTokenException();
        });
        if (current.isRevoked()) {
            int killed = tokens.revokeFamily(current.getFamilyId(), now);
            count("reused");
            log.warn("Refresh token reuse for user {}: session revoked ({} live token(s))", current.getUserId(), killed);
            throw new InvalidRefreshTokenException();
        }
        if (current.getExpiresAt().isBefore(now)) {
            count("expired");
            throw new InvalidRefreshTokenException();
        }
        User user = users.findById(current.getUserId()).orElseThrow(() -> {
            count("no_user");
            return new InvalidRefreshTokenException();
        });
        current.revoke(now);
        count("rotated");
        return new Rotation(user, issue(user.getId(), current.getFamilyId()));
    }

    /** Logout: end this session (all its tokens). Unknown tokens are ignored. */
    @Transactional
    public void endSession(String raw) {
        tokens.findByTokenHash(hash(raw)).ifPresent(t -> tokens.revokeFamily(t.getFamilyId(), clock.instant()));
    }

    /** Account deletion: every session of this user. */
    @Transactional
    public int deleteAllOf(UUID userId) {
        return tokens.deleteAllOfUser(userId);
    }

    /** Housekeeping: drop rows that can never be used again (kept 7 more days to still detect late reuse). */
    @Scheduled(cron = "0 17 3 * * *")
    @Transactional
    public void purgeExpired() {
        int n = tokens.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(7)));
        if (n > 0) log.info("Purged {} expired refresh token(s)", n);
    }

    private String issue(UUID userId, UUID familyId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        tokens.save(new RefreshToken(userId, familyId, hash(raw), now, now.plus(ttl)));
        return raw;
    }

    private void count(String result) {
        metrics.counter("softlaunch.sessions.refresh", "result", result).increment();
    }

    static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
