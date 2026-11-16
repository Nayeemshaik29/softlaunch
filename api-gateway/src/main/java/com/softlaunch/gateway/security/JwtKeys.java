package com.softlaunch.gateway.security;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

/**
 * Builds the JWT signing key and refuses to start with a missing or weak secret.
 * Fail fast: a service that won't start is safe, one that starts with an empty or guessable key is not.
 * The secret comes from the JWT_SECRET environment variable (.env locally, never git).
 */
final class JwtKeys {

    private static final int MIN_BYTES = 32;   // 256 bits, the minimum for HS256

    private JwtKeys() {
    }

    static SecretKey fromBase64(String secret) {
        // @ConfigurationProperties leaves an unresolved "${JWT_SECRET}" as plain text instead of failing, so check for it
        if (secret == null || secret.isBlank() || secret.contains("${")) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set. Add it to the .env file in the project root (see .env.example) "
                            + "or set it as an environment variable. Generate one with: openssl rand -base64 32");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret.trim());
        } catch (DecodingException e) {
            throw new IllegalStateException("JWT_SECRET must be Base64 (generate one with: openssl rand -base64 32)");
        }
        if (bytes.length < MIN_BYTES) {
            throw new IllegalStateException("JWT_SECRET is too short: " + bytes.length + " bytes, need at least "
                    + MIN_BYTES + " (generate one with: openssl rand -base64 32)");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
