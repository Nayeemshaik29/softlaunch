package com.softlaunch.chat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.UUID;

/**
 * WebSocket logins don't pass the gateway's JWT check (/ws/ is public there), so chat checks tokens itself,
 * including the "account deleted" mark user-service writes to Redis (revoked:user:<id>).
 */
@Component
public class JwtVerifier {

    private static final Logger log = LoggerFactory.getLogger(JwtVerifier.class);
    private static final String REVOKED_PREFIX = "revoked:user:";

    private final SecretKey key;
    private final StringRedisTemplate redis;

    public JwtVerifier(@Value("${softlaunch.jwt.secret}") String secret, StringRedisTemplate redis) {
        this.key = JwtKeys.fromBase64(secret);
        this.redis = redis;
    }

    public UUID verify(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        UUID userId = UUID.fromString(claims.getSubject());
        if (isRevoked(userId)) {
            throw new IllegalStateException("account deleted");
        }
        return userId;
    }

    /** Fail-open, same reasoning as the gateway: a Redis hiccup must not log out everyone. */
    private boolean isRevoked(UUID userId) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(REVOKED_PREFIX + userId));
        } catch (RuntimeException e) {
            log.warn("Revocation check skipped ({})", e.getClass().getSimpleName());
            return false;
        }
    }
}
