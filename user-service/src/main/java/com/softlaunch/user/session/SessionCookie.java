package com.softlaunch.user.session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * The refresh token travels in a cookie the page's JavaScript can't read (HttpOnly): an XSS bug can't steal it.
 * SameSite=Strict: never sent when another site makes the request. Path: only our auth endpoints ever see it.
 * Secure (HTTPS only) is ON in production; off locally because localhost is plain HTTP.
 */
@Component
public class SessionCookie {

    public static final String NAME = "sl_refresh";
    private static final String PATH = "/api/users/auth";

    private final boolean secure;
    private final Duration ttl;

    public SessionCookie(@Value("${softlaunch.auth.refresh-cookie-secure:true}") boolean secure,
                         @Value("${softlaunch.auth.refresh-ttl:30d}") Duration ttl) {
        this.secure = secure;
        this.ttl = ttl;
    }

    public String set(String refreshToken) {
        return base(refreshToken).maxAge(ttl).build().toString();
    }

    public String clear() {
        return base("").maxAge(0).build().toString();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value).httpOnly(true).secure(secure).sameSite("Strict").path(PATH);
    }
}
