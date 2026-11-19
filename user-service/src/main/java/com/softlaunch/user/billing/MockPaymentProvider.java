package com.softlaunch.user.billing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * LOCAL / CI ONLY (softlaunch.billing.provider=mock, the default). No money moves. The "payment" is made by
 * POST /api/users/me/billing/mock-pay, which only exists while this provider is active.
 */
@Component
@ConditionalOnProperty(name = "softlaunch.billing.provider", havingValue = "mock", matchIfMissing = true)
class MockPaymentProvider implements PaymentProvider {

    static final String SECRET = "mock-provider-not-a-real-secret";

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public ProviderOrder createOrder(Purchase purchase) {
        return new ProviderOrder("mock_order_" + UUID.randomUUID().toString().replace("-", ""), Map.of());
    }

    /** What the fake checkout returns: a payment id + the same kind of signature Razorpay sends. */
    String[] pay(String orderId) {
        String paymentId = "mock_pay_" + UUID.randomUUID().toString().replace("-", "");
        return new String[]{paymentId, Hmac.sha256Hex(SECRET, orderId + "|" + paymentId)};
    }

    @Override
    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        return Hmac.matches(Hmac.sha256Hex(SECRET, orderId + "|" + paymentId), signature);
    }

    @Override
    public boolean verifyWebhook(String rawBody, String signature) {
        return false;   // no webhooks in mock mode
    }
}
