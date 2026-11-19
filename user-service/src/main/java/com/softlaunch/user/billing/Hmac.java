package com.softlaunch.user.billing;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

final class Hmac {
    private Hmac() {
    }

    static String sha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Constant-time compare: response timing can't leak how much of a forged signature was right. */
    static boolean matches(String expectedHex, String givenHex) {
        if (givenHex == null) return false;
        return MessageDigest.isEqual(expectedHex.getBytes(StandardCharsets.UTF_8), givenHex.getBytes(StandardCharsets.UTF_8));
    }
}
