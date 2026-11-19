package com.softlaunch.user.session;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The refresh token may only be handed out in a response body to the phone apps, never to a web page. */
class NativeClientsTest {

    private final NativeClients clients = new NativeClients(List.of("capacitor://localhost", "https://localhost"));

    private static MockHttpServletRequest req(String origin, String transport) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        if (origin != null) r.addHeader("Origin", origin);
        if (transport != null) r.addHeader(NativeClients.TRANSPORT_HEADER, transport);
        return r;
    }

    @Test
    void iosAndAndroidAppsGetTheTokenInTheBody() {
        assertThat(clients.wantsBody(req("capacitor://localhost", "body"))).isTrue();
        assertThat(clients.wantsBody(req("https://localhost", "body"))).isTrue();
    }

    @Test
    void aWebPageCantAskForIt() {
        assertThat(clients.wantsBody(req("https://softlaunch.example", "body"))).isFalse();   // e.g. an XSS on our site
        assertThat(clients.wantsBody(req(null, "body"))).isFalse();
        assertThat(clients.wantsBody(req("capacitor://localhost", null))).isFalse();
    }
}
