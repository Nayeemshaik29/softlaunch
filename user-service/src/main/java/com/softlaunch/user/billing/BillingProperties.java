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

    /** A Plus pass (period set) or a Hearts pack (hearts > 0, no period). */
    public record Plan(String id, String name, long amountPaise, Duration period, int hearts) {
        public boolean isHearts() {
            return hearts > 0;
        }

        /** Plus comes with 20 ❤ per 30 days of pass (week: 5, year: 240). */
        public int bonusHearts() {
            return period == null ? 0 : (int) Math.max(5, Math.round(period.toDays() * 20 / 30.0));
        }
    }

    public record Razorpay(String keyId, String keySecret, String webhookSecret, String apiUrl) {
    }

    public Plan plan(String id) {
        return plans.stream().filter(p -> p.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown plan"));
    }
}
