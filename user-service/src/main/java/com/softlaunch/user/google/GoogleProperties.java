package com.softlaunch.user.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Sign in with Google. clientIds = the OAuth client IDs whose ID tokens we accept (the "aud" claim):
 * the web client now; the Android/iOS client IDs later. Empty = Google sign-in is switched off.
 * Client IDs are public (they're in the web page anyway); there is NO client secret in this flow.
 */
@ConfigurationProperties(prefix = "softlaunch.google")
public record GoogleProperties(List<String> clientIds, String jwksUrl) {

    public GoogleProperties {
        clientIds = clientIds == null ? List.of() : clientIds.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (jwksUrl == null || jwksUrl.isBlank()) {
            jwksUrl = "https://www.googleapis.com/oauth2/v3/certs";
        }
    }

    public boolean enabled() {
        return !clientIds.isEmpty();
    }
}
