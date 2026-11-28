package com.softlaunch.chat.call;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * WebRTC helpers. stunUrls: public address discovery. turnUrls + turnSecret: our coturn relay, used when phones can't
 * reach each other directly (common on mobile data). Credentials are short-lived (coturn "REST API" scheme):
 * username = expiry:userId, password = base64(HMAC-SHA1(secret, username)). The secret never leaves the server.
 */
@ConfigurationProperties(prefix = "softlaunch.calls")
public record CallProperties(List<String> stunUrls, List<String> turnUrls, String turnSecret, Duration credentialTtl,
                             Duration ringTimeout, Duration maxCall) {
    public CallProperties {
        stunUrls = stunUrls == null ? List.of() : stunUrls.stream().filter(s -> !s.isBlank()).toList();
        turnUrls = turnUrls == null ? List.of() : turnUrls.stream().filter(s -> !s.isBlank()).toList();
        if (credentialTtl == null) credentialTtl = Duration.ofHours(1);
        if (ringTimeout == null) ringTimeout = Duration.ofSeconds(45);
        if (maxCall == null) maxCall = Duration.ofHours(4);
    }
}
