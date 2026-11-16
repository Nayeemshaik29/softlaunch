package com.softlaunch.chat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.UUID;

@Component
public class JwtVerifier {

    private final SecretKey key;

    public JwtVerifier(@Value("${softlaunch.jwt.secret}") String secret) {
        this.key = JwtKeys.fromBase64(secret);
    }

    public UUID verify(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return UUID.fromString(claims.getSubject());
    }
}