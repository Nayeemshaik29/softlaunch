package com.softlaunch.user.google;

import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.security.Key;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Google's public signing keys (JWKS), cached. Google rotates them every few weeks and publishes the new one early,
 * so: refetch every 6 hours, or straight away for an unknown key id, but at most once a minute
 * (a flood of tokens with made-up key ids must not turn into a flood of calls to Google).
 */
@Component
public class GoogleKeys {

    private static final Duration MAX_AGE = Duration.ofHours(6);
    private static final Duration MIN_REFETCH = Duration.ofMinutes(1);

    private final RestClient http;
    private final String url;
    private final Clock clock;
    private volatile Map<String, Key> keys = Map.of();
    private volatile Instant fetchedAt = Instant.EPOCH;

    @Autowired
    public GoogleKeys(GoogleProperties props) {
        this(RestClient.builder().build(), props.jwksUrl(), Clock.systemUTC());
    }

    GoogleKeys(RestClient http, String url, Clock clock) {
        this.http = http;
        this.url = url;
        this.clock = clock;
    }

    public Key find(String kid) {
        if (kid == null) {
            return null;
        }
        Instant now = clock.instant();
        Key key = keys.get(kid);
        boolean stale = fetchedAt.plus(MAX_AGE).isBefore(now);
        boolean mayRefetch = fetchedAt.plus(MIN_REFETCH).isBefore(now);
        if ((key == null || stale) && mayRefetch) {
            refresh(now);
            key = keys.get(kid);
        }
        return key;
    }

    private synchronized void refresh(Instant now) {
        if (fetchedAt.plus(MIN_REFETCH).isAfter(now)) {
            return; // another thread just did it
        }
        fetchedAt = now; // even on failure: don't hammer Google while it's down
        String body = http.get().uri(url).retrieve().body(String.class);
        JwkSet set = Jwks.setParser().build().parse(body);
        Map<String, Key> next = new HashMap<>();
        for (Jwk<?> jwk : set.getKeys()) {
            if (jwk.getId() != null) {
                next.put(jwk.getId(), jwk.toKey());
            }
        }
        keys = Map.copyOf(next);
    }
}
