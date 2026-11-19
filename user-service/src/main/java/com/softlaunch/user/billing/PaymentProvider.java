package com.softlaunch.user.billing;

import java.util.Map;

/** Razorpay in production; a fake for local development and CI. */
public interface PaymentProvider {

    String name();

    /** Creates the provider-side order. Returns its id + what the browser's checkout needs (public data only). */
    ProviderOrder createOrder(Purchase purchase);

    /** Checkout result signature from the browser. Must be checked on the server: the browser can be lied to. */
    boolean verifyPayment(String orderId, String paymentId, String signature);

    /** Webhook body signature (provider → us). */
    boolean verifyWebhook(String rawBody, String signature);

    record ProviderOrder(String orderId, Map<String, Object> checkout) {
    }
}
