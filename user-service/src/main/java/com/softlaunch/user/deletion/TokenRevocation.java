package com.softlaunch.user.deletion;

import com.softlaunch.user.service.JwtService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * A JWT stays valid until it expires: the server keeps no list of "logged in" tokens.
 * So after deletion we write revoked:user:<id> to Redis, and the gateway (and chat's WebSocket login)
 * refuse every token of that user. The key only has to live as long as the longest token.
 */
@Component
public class TokenRevocation {

    public static final String PREFIX = "revoked:user:";

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public TokenRevocation(StringRedisTemplate redis, JwtService jwtService) {
        this.redis = redis;
        this.ttl = jwtService.getExpiration().plusMinutes(1);   // +1 min for clock differences
    }

    public void revokeAllTokensOf(UUID userId) {
        redis.opsForValue().set(PREFIX + userId, "deleted", ttl);
    }
}
