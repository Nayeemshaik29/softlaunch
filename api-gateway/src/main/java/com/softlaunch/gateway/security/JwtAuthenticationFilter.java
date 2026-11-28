package com.softlaunch.gateway.security;

import com.softlaunch.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    /** Written by user-service when an account is deleted (see TokenRevocation there). */
    private static final String REVOKED_PREFIX = "revoked:user:";
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/users/auth/",
            "/ws/",
            "/api/verification/digilocker/callback",
            "/api/users/billing/webhook/"   // Razorpay → us; trusted only through its HMAC signature
    );

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private final Map<String, Long> lastTouch = new ConcurrentHashMap<>();
    private final SecretKey key;
    private final ReactiveStringRedisTemplate redis;

    public JwtAuthenticationFilter(JwtProperties properties, ReactiveStringRedisTemplate redis) {
        this.key = JwtKeys.fromBase64(properties.secret());
        this.redis = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isPublic(path)) {
            return chain.filter(withoutUserHeader(exchange));
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring("Bearer ".length());

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .headers(headers -> headers.set(USER_ID_HEADER, userId))
                    .build();

            return isRevoked(userId).flatMap(revoked -> {
                if (revoked) {
                    return unauthorized(exchange);   // account deleted or suspended: its tokens die now, not in 15 minutes
                }
                touch(userId);
                return chain.filter(exchange.mutate().request(request).build());
            });
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange);
        }
    }

    /**
     * Admin dashboard: "active today" (act:day:{day}) and "last active" (act:recent). At most once a minute per user,
     * fire-and-forget: never slows or fails the request.
     */
    private void touch(String userId) {
        long now = System.currentTimeMillis();
        Long prev = lastTouch.get(userId);
        if (prev != null && now - prev < 60_000) {
            return;
        }
        if (lastTouch.size() > 100_000) {
            lastTouch.clear();   // bounded memory; worst case a few extra Redis writes
        }
        lastTouch.put(userId, now);
        String dayKey = "act:day:" + LocalDate.now(IST);
        redis.opsForSet().add(dayKey, userId)
                .then(redis.expire(dayKey, Duration.ofDays(40)))
                .then(redis.opsForZSet().add("act:recent", userId, now))
                .timeout(Duration.ofSeconds(1))
                .subscribe(ok -> { }, e -> log.debug("activity touch skipped ({})", e.getClass().getSimpleName()));
    }

    /**
     * One Redis lookup per request (~0.2 ms). Fail-open: if Redis is down we let the token through,
     * because the deleted user's data is already gone everywhere (they would only see empty/404 answers),
     * while failing closed would log out EVERY user during a Redis hiccup.
     */
    private Mono<Boolean> isRevoked(String userId) {
        return redis.hasKey(REVOKED_PREFIX + userId)
                .timeout(Duration.ofMillis(300))
                .onErrorResume(e -> {
                    log.warn("Revocation check skipped ({})", e.getClass().getSimpleName());
                    return Mono.just(false);
                });
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private ServerWebExchange withoutUserHeader(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.remove(USER_ID_HEADER))
                .build();
        return exchange.mutate().request(request).build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}