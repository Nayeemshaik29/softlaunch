package com.softlaunch.user.billing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Razorpay's documented signature scheme: HMAC-SHA256(order_id + "|" + payment_id, key_secret), hex. */
class RazorpaySignatureTest {

    @Test
    void matchesAnIndependentlyComputedHmac() throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec("test_secret".getBytes(), "HmacSHA256"));
        String expected = java.util.HexFormat.of().formatHex(mac.doFinal("order_ABC|pay_XYZ".getBytes()));
        assertThat(Hmac.sha256Hex("test_secret", "order_ABC|pay_XYZ")).isEqualTo(expected);
        assertThat(Hmac.matches(expected, expected)).isTrue();
        assertThat(Hmac.matches(expected, expected.replace('a', 'b'))).isEqualTo(!expected.contains("a"));
        assertThat(Hmac.matches(expected, null)).isFalse();
    }
}
