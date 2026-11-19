package com.softlaunch.user.session;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The iOS / Android apps (Capacitor) run their pages from capacitor://localhost / https://localhost, a different site
 * than the API, so the strict refresh cookie can't work there. They ask for the refresh token in the response body
 * (header X-Refresh-Transport: body) and keep it in the Keychain / Keystore.
 *
 * Only requests whose Origin is one of the APP origins may do that: a normal web page (even one with an XSS bug)
 * can't turn its HttpOnly cookie into a readable token.
 */
@Component
public class NativeClients {

    public static final String TRANSPORT_HEADER = "X-Refresh-Transport";
    public static final String TOKEN_HEADER = "X-Refresh-Token";

    private final List<String> nativeOrigins;

    public NativeClients(@Value("${softlaunch.auth.native-origins:capacitor://localhost,https://localhost}") List<String> nativeOrigins) {
        this.nativeOrigins = nativeOrigins;
    }

    public boolean wantsBody(HttpServletRequest request) {
        String origin = request.getHeader("Origin");   // no Origin (e.g. curl, server-to-server) = not an app
        return origin != null && "body".equalsIgnoreCase(request.getHeader(TRANSPORT_HEADER)) && nativeOrigins.contains(origin);
    }
}
