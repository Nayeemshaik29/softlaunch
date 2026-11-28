package com.softlaunch.user.google;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleTokenVerifierTest {

    private static final String CLIENT = "123-web.apps.googleusercontent.com";
    private final KeyPair google = Jwts.SIG.RS256.keyPair().build();
    private final KeyPair attacker = Jwts.SIG.RS256.keyPair().build();

    private final GoogleKeys keys = new GoogleKeys(RestClient.create(), "unused", Clock.systemUTC()) {
        @Override
        public Key find(String kid) {
            return "k1".equals(kid) ? google.getPublic() : null;
        }
    };
    private final GoogleTokenVerifier verifier = new GoogleTokenVerifier(keys, new GoogleProperties(List.of(CLIENT), null));

    private String token(KeyPair kp, String kid, Map<String, Object> override) {
        var b = Jwts.builder().header().keyId(kid).and()
                .issuer("https://accounts.google.com").audience().add(CLIENT).and()
                .subject("google-sub-1").claim("email", "Nisha@Gmail.com").claim("email_verified", true)
                .claim("name", "Nisha Rao").issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(600)));
        override.forEach((k, v) -> {
            if ("aud".equals(k)) {
                b.audience().single((String) v);
            } else {
                b.claim(k, v);
            }
        });
        return b.signWith(kp.getPrivate(), Jwts.SIG.RS256).compact();
    }

    @Test
    void acceptsAGenuineToken() {
        GoogleIdentity id = verifier.verify(token(google, "k1", Map.of()));
        assertThat(id.sub()).isEqualTo("google-sub-1");
        assertThat(id.email()).isEqualTo("nisha@gmail.com");
        assertThat(id.name()).isEqualTo("Nisha Rao");
    }

    @Test
    void refusesATokenSignedByAnyoneElse() {
        assertThatThrownBy(() -> verifier.verify(token(attacker, "k1", Map.of()))).isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    void refusesUnknownKeyIds() {
        assertThatThrownBy(() -> verifier.verify(token(google, "made-up", Map.of()))).isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    void refusesTokensIssuedForAnotherApp() {
        assertThatThrownBy(() -> verifier.verify(token(google, "k1", Map.of("aud", "someone-else.apps.googleusercontent.com"))))
                .isInstanceOf(InvalidGoogleTokenException.class).hasMessageContaining("not for us");
    }

    @Test
    void refusesOtherIssuers() {
        assertThatThrownBy(() -> verifier.verify(token(google, "k1", Map.of("iss", "https://evil.example"))))
                .isInstanceOf(InvalidGoogleTokenException.class).hasMessageContaining("issuer");
    }

    @Test
    void refusesUnverifiedEmails() {
        assertThatThrownBy(() -> verifier.verify(token(google, "k1", Map.of("email_verified", false))))
                .isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    void refusesExpiredTokens() {
        String expired = token(google, "k1", Map.of("exp", Instant.now().minusSeconds(3600).getEpochSecond()));
        assertThatThrownBy(() -> verifier.verify(expired)).isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    void refusesHmacTokens() {
        SecretKey hmac = Jwts.SIG.HS256.key().build();
        String forged = Jwts.builder().header().keyId("k1").and().issuer("accounts.google.com").audience().add(CLIENT).and()
                .subject("x").claim("email", "a@b.c").claim("email_verified", true)
                .expiration(Date.from(Instant.now().plusSeconds(600))).signWith(hmac).compact();
        assertThatThrownBy(() -> verifier.verify(forged)).isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    void refusesGarbage() {
        assertThatThrownBy(() -> verifier.verify("not.a.jwt")).isInstanceOf(InvalidGoogleTokenException.class);
    }
}
