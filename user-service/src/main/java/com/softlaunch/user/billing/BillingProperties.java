package com.softlaunch.user.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * softlaunch.billing.* (config-repo/user-service.yaml). Prices are in paise (₹1 = 100 paise), as payment APIs expect.
 * provider: mock (local/CI, fake checkout) or razorpay (needs your own keys in .env).
 */
@ConfigurationProperties("softlaunch.billing")
public record BillingProperties(String provider, String currency, List<Plan> plans, Razorpay razorpay) {

    public record Plan(String id, String name, long amountPaise, Duration period) {
    }

    public record Razorpay(String keyId, String keySecret, String webhookSecret, String apiUrl) {
    }

    public Plan plan(String id) {
        return plans.stream().filter(p -> p.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown plan"));
    }
}
