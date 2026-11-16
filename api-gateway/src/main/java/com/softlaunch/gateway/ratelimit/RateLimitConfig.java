package com.softlaunch.gateway.ratelimit;

import com.softlaunch.gateway.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.support.ipresolver.RemoteAddressResolver;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * Who does a request "belong to" for rate limiting? Each KeyResolver answers that, and Redis keeps
 * one token bucket per answer. Prefixes ("auth:", "user:", "anon:") keep the buckets of the
 * different limiters apart, because Redis keys don't include the route.
 */
@Configuration(proxyBeanMethods = false)
class RateLimitConfig {

    private final RemoteAddressResolver addressResolver;

    /**
     * trusted-proxies = how many proxies in front of us add an X-Forwarded-For entry.
     * Locally 0: use the real TCP address (an attacker can put anything in X-Forwarded-For).
     * Behind one cloud load balancer: 1, so we take the address that load balancer saw.
     */
    RateLimitConfig(@Value("${softlaunch.rate-limit.trusted-proxies:0}") int trustedProxies) {
        this.addressResolver = trustedProxies > 0
                ? XForwardedRemoteAddressResolver.maxTrustedIndex(trustedProxies)
                : new RemoteAddressResolver() { };
    }

    /** Login / signup: by IP, because there is no user yet. Strict limit, see api-gateway.yaml. */
    @Bean
    KeyResolver authKeyResolver() {
        return exchange -> Mono.just("auth:" + clientIp(exchange));
    }

    /** Everything else: by user id (set by JwtAuthenticationFilter, which runs first). Public paths fall back to IP. */
    @Bean
    @Primary
    KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst(JwtAuthenticationFilter.USER_ID_HEADER);
            return Mono.just(userId != null ? "user:" + userId : "anon:" + clientIp(exchange));
        };
    }

    /** AI routes: per user, with its own "ai:" bucket (LLM calls are slow and expensive). */
    @Bean
    KeyResolver aiKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst(JwtAuthenticationFilter.USER_ID_HEADER);
            return Mono.just("ai:" + (userId != null ? userId : clientIp(exchange)));
        };
    }

    private String clientIp(ServerWebExchange exchange) {
        InetSocketAddress address = addressResolver.resolve(exchange);
        return address == null || address.getAddress() == null ? "unknown" : address.getAddress().getHostAddress();
    }
}
