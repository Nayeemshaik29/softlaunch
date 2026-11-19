package com.softlaunch.user.billing;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final BillingProperties props;
    private final PaymentProvider provider;
    private final PurchaseRepository purchases;
    private final PlusMembershipRepository memberships;
    private final PlusEntitlement entitlement;
    private final MeterRegistry metrics;
    private final JsonMapper json;

    public BillingService(BillingProperties props, PaymentProvider provider, PurchaseRepository purchases,
                          PlusMembershipRepository memberships, PlusEntitlement entitlement, MeterRegistry metrics, JsonMapper json) {
        this.props = props;
        this.provider = provider;
        this.purchases = purchases;
        this.memberships = memberships;
        this.entitlement = entitlement;
        this.metrics = metrics;
        this.json = json;
    }

    public record PlanView(String id, String name, long amountPaise, String currency, long days) {
    }

    public record Status(boolean plus, Instant activeUntil, String planId, String provider) {
    }

    public record Checkout(String orderId, String provider, long amountPaise, String currency, String planName, Map<String, Object> checkout) {
    }

    public List<PlanView> plans() {
        return props.plans().stream()
                .map(p -> new PlanView(p.id(), p.name(), p.amountPaise(), props.currency(), p.period().toDays())).toList();
    }

    @Transactional(readOnly = true)
    public Status status(UUID userId) {
        return memberships.findById(userId)
                .filter(m -> m.getActiveUntil().isAfter(Instant.now()))
                .map(m -> new Status(true, m.getActiveUntil(), m.getLastPlanId(), provider.name()))
                .orElse(new Status(false, null, null, provider.name()));
    }

    @Transactional
    public Checkout checkout(UUID userId, String planId) {
        BillingProperties.Plan plan;
        try {
            plan = props.plan(planId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown plan");
        }
        Purchase purchase = new Purchase(userId, plan, props.currency(), provider.name());
        PaymentProvider.ProviderOrder order;
        try {
            order = provider.createOrder(purchase);
        } catch (RuntimeException e) {
            log.warn("Payment provider unavailable ({})", e.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payments are unavailable right now. Please try again.");
        }
        purchase.setProviderOrderId(order.orderId());
        purchases.save(purchase);
        metrics.counter("softlaunch.billing.checkouts", "plan", plan.id()).increment();
        return new Checkout(order.orderId(), provider.name(), plan.amountPaise(), props.currency(), plan.name(), order.checkout());
    }

    /** From the browser after checkout. Only the owner of the order can confirm it, and only with a valid signature. */
    @Transactional
    public Status confirm(UUID userId, String orderId, String paymentId, String signature) {
        if (!provider.verifyPayment(orderId, paymentId, signature)) {
            metrics.counter("softlaunch.billing.payments", "result", "bad_signature").increment();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "We couldn't verify that payment.");
        }
        Purchase p = purchases.findByProviderOrderId(orderId)
                .filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        activate(p, paymentId);
        return status(userId);
    }

    /** Razorpay → us, for payments whose browser tab closed before confirming. */
    @Transactional
    public void webhook(String rawBody, String signature) {
        if (!provider.verifyWebhook(rawBody, signature)) {
            metrics.counter("softlaunch.billing.webhooks", "result", "bad_signature").increment();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad signature");
        }
        JsonNode event = json.readTree(rawBody);
        String type = event.path("event").asString("");
        if (!type.equals("payment.captured") && !type.equals("order.paid")) return;   // others are ignored
        JsonNode payment = event.path("payload").path("payment").path("entity");
        String orderId = payment.path("order_id").asString("");
        purchases.findByProviderOrderId(orderId).ifPresent(p -> activate(p, payment.path("id").asString("")));
        metrics.counter("softlaunch.billing.webhooks", "result", type).increment();
    }

    /** Mock provider only: plays the part of the checkout window. */
    @Transactional
    public Status mockPay(UUID userId, String orderId) {
        if (!(provider instanceof MockPaymentProvider mock)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String[] paid = mock.pay(orderId);
        return confirm(userId, orderId, paid[0], paid[1]);
    }

    private void activate(Purchase p, String paymentId) {
        if (!p.markPaid(paymentId)) return;   // already done (browser + webhook both arrived): no double extension
        PlusMembership m = memberships.findForUpdate(p.getUserId()).orElseGet(() -> new PlusMembership(p.getUserId()));
        m.extend(props.plan(p.getPlanId()), Instant.now());
        memberships.save(m);
        entitlement.activeUntil(p.getUserId(), m.getActiveUntil());
        metrics.counter("softlaunch.billing.payments", "result", "paid", "plan", p.getPlanId()).increment();
        log.info("Plus activated for {} until {} (purchase {})", p.getUserId(), m.getActiveUntil(), p.getId());   // ids only
    }

    /** Account deletion: membership + entitlement go; purchase records stay (tax/refunds) and hold only ids/amounts. */
    @Transactional
    public void forget(UUID userId) {
        memberships.deleteById(userId);
        entitlement.forget(userId);
    }
}
