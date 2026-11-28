package com.softlaunch.chat.call;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CallRulesTest {

    @Test
    void aCallOnlyGetsWhatBothAgreedTo() {
        assertThat(CallMode.lowerOf(CallMode.VIDEO, CallMode.VIDEO)).isEqualTo(CallMode.VIDEO);
        assertThat(CallMode.lowerOf(CallMode.VIDEO, CallMode.AUDIO)).isEqualTo(CallMode.AUDIO);
        assertThat(CallMode.lowerOf(CallMode.AUDIO, CallMode.VIDEO)).isEqualTo(CallMode.AUDIO);
        assertThat(CallMode.lowerOf(CallMode.VIDEO, CallMode.OFF)).isEqualTo(CallMode.OFF);
        assertThat(CallMode.lowerOf(CallMode.OFF, CallMode.OFF)).isEqualTo(CallMode.OFF);
    }

    @Test
    void turnPasswordMatchesCoturnsFormula() {
        // same as: printf '1700000000:user-1' | openssl dgst -sha1 -hmac test-secret -binary | base64
        assertThat(CallService.hmac("test-secret", "1700000000:user-1")).isEqualTo("HcBAu/wxU05xBRroXBmdx9PMA5E=");
    }

    @Test
    void noRelayWithoutASecret() {
        CallService calls = new CallService(null, null, null,
                new CallProperties(List.of("stun:x:3478"), List.of("turn:x:3478"), "", null, null, null), null, null);
        var ice = calls.iceServers(java.util.UUID.randomUUID());
        assertThat(ice.iceServers()).hasSize(1);
        assertThat(ice.iceServers().get(0).username()).isNull();
    }
}
