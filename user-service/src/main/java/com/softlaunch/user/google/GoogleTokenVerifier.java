package com.softlaunch.user.google;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Set;

/**
 * Checks a Google ID token ourselves (no call to Google per sign-in):
 * RS256 signature by one of Google's current keys, not expired, issued by Google, issued FOR US (aud = one of our
 * client IDs), and the email is verified by Google. Only then do we trust "sub" and "email".
 */
@Component
public class GoogleTokenVerifier {

    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final GoogleKeys keys;
    private final GoogleProperties props;

    public GoogleTokenVerifier(GoogleKeys keys, GoogleProperties props) {
        this.keys = keys;
        this.props = props;
    }

    public GoogleIdentity verify(String idToken) {
        Claims c;
        try {
            c = Jwts.parser()
                    .keyLocator(new LocatorAdapter<Key>() {
                        @Override
                        protected Key locate(JwsHeader header) {
                            if (!"RS256".equals(header.getAlgorithm())) {
                                throw new InvalidGoogleTokenException("unexpected alg");
                            }
                            Key k = keys.find(header.getKeyId());
                            if (k == null) {
                                throw new InvalidGoogleTokenException("unknown key id");
                            }
                            return k;
                        }
                    })
                    .clockSkewSeconds(60)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidGoogleTokenException("bad token: " + e.getClass().getSimpleName());
        }
        if (!ISSUERS.contains(c.getIssuer())) {
            throw new InvalidGoogleTokenException("wrong issuer");
        }
        Set<String> aud = c.getAudience();
        if (aud == null || aud.stream().noneMatch(props.clientIds()::contains)) {
            throw new InvalidGoogleTokenException("not for us");
        }
        if (c.getExpiration() == null) {
            throw new InvalidGoogleTokenException("no expiry");
        }
        String sub = c.getSubject();
        String email = c.get("email", String.class);
        Object verified = c.get("email_verified");
        if (sub == null || sub.isBlank() || email == null || !(Boolean.TRUE.equals(verified) || "true".equals(verified))) {
            throw new InvalidGoogleTokenException("email not verified");
        }
        return new GoogleIdentity(sub, email.trim().toLowerCase(), c.get("name", String.class));
    }
}
