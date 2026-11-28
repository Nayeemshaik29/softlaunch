package com.softlaunch.user.billing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * Razorpay Orders API (India: UPI, cards, wallets). Keys come from .env (RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET /
 * RAZORPAY_WEBHOOK_SECRET); use TEST keys until launch. The key SECRET never leaves this service.
 * Signatures (Razorpay docs): payment = HMAC_SHA256(order_id + "|" + payment_id, key_secret);
 * webhook = HMAC_SHA256(raw body, webhook_secret).
 */
@Component
@ConditionalOnProperty(name = "softlaunch.billing.provider", havingValue = "razorpay")
class RazorpayPaymentProvider implements PaymentProvider {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RazorpayPaymentProvider.class);

    private final BillingProperties.Razorpay cfg;
    private final RestClient http;

    RazorpayPaymentProvider(BillingProperties props) {
        this.cfg = props.razorpay();
        if (cfg == null || blank(cfg.keyId()) || blank(cfg.keySecret()) || blank(cfg.webhookSecret())) {
            throw new IllegalStateException("softlaunch.billing.provider=razorpay needs RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET and RAZORPAY_WEBHOOK_SECRET");
        }
        if (cfg.keyId().startsWith("rzp_live_")) {
            LOG.warn("Razorpay LIVE mode: payments take real money");
        } else if (cfg.keyId().startsWith("rzp_test_")) {
            LOG.info("Razorpay TEST mode: no real money moves");
        } else {
            throw new IllegalStateException("RAZORPAY_KEY_ID must start with rzp_test_ or rzp_live_");
        }
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(3));
        f.setReadTimeout(Duration.ofSeconds(10));
        this.http = RestClient.builder().requestFactory(f)
                .baseUrl(blank(cfg.apiUrl()) ? "https://api.razorpay.com" : cfg.apiUrl())
                .defaultHeaders(h -> h.setBasicAuth(cfg.keyId(), cfg.keySecret()))
                .build();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    @Override
    public String name() {
        return "razorpay";
    }

    @Override
    public ProviderOrder createOrder(Purchase purchase) {
        Map<?, ?> order = http.post().uri("/v1/orders").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", purchase.getAmountPaise(), "currency", purchase.getCurrency(),
                        "receipt", purchase.getId().toString().substring(0, 32),   // Razorpay: max 40 chars
                        "notes", Map.of("plan", purchase.getPlanId())))
                .retrieve().body(Map.class);
        String orderId = String.valueOf(order.get("id"));
        // public data for Razorpay Checkout in the browser (the key ID is public by design; the secret is not)
        return new ProviderOrder(orderId, Map.of("keyId", cfg.keyId()));
    }

    @Override
    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        return Hmac.matches(Hmac.sha256Hex(cfg.keySecret(), orderId + "|" + paymentId), signature);
    }

    @Override
    public boolean verifyWebhook(String rawBody, String signature) {
        return Hmac.matches(Hmac.sha256Hex(cfg.webhookSecret(), rawBody), signature);
    }
}
