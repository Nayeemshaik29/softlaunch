package com.softlaunch.user.privacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * "Are these two people matched?" asked to match-service. Used to decide whether a veiled profile's real photos
 * may be shown. FAILS CLOSED: if match-service doesn't answer, the photos stay veiled.
 * (Own RestClient + LoadBalancerClient on purpose: a shared @LoadBalanced builder bean could be picked up by the
 * Eureka client itself.)
 */
@Component
public class MatchChecker {

    private static final Logger log = LoggerFactory.getLogger(MatchChecker.class);
    private final LoadBalancerClient loadBalancer;
    private final RestClient http;

    public MatchChecker(LoadBalancerClient loadBalancer) {
        this.loadBalancer = loadBalancer;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofMillis(500));
        f.setReadTimeout(Duration.ofSeconds(1));
        this.http = RestClient.builder().requestFactory(f).build();
    }

    public boolean matched(UUID a, UUID b) {
        try {
            ServiceInstance match = loadBalancer.choose("match-service");
            if (match == null) throw new IllegalStateException("no match-service instance");
            Map<?, ?> body = http.get()
                    .uri(match.getUri() + "/internal/matches/between?a={a}&b={b}", a, b)
                    .retrieve().body(Map.class);
            return body != null && Boolean.TRUE.equals(body.get("matched"));
        } catch (RuntimeException e) {
            log.warn("Match check failed ({}); keeping photos veiled", e.getClass().getSimpleName());
            return false;
        }
    }
}
