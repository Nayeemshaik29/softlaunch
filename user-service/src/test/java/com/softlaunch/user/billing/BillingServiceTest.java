package com.softlaunch.user.billing;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BillingServiceTest {

    private final BillingProperties props = new BillingProperties("mock", "INR",
            List.of(new BillingProperties.Plan("plus-month", "Plus · 1 month", 9900, Duration.ofDays(30), 0),
                    new BillingProperties.Plan("hearts-30", "30 Hearts", 12900, null, 30)), null);
    private final com.softlaunch.user.hearts.HeartService hearts = mock(com.softlaunch.user.hearts.HeartService.class);
    private final MockPaymentProvider provider = new MockPaymentProvider();
    private final PurchaseRepository purchases = mock(PurchaseRepository.class);
    private final PlusMembershipRepository memberships = mock(PlusMembershipRepository.class);
    private final PlusEntitlement entitlement = mock(PlusEntitlement.class);
    private final Map<String, Purchase> orders = new HashMap<>();
    private final Map<UUID, PlusMembership> members = new HashMap<>();
    private BillingService billing;
    private final UUID me = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        billing = new BillingService(props, provider, purchases, memberships, entitlement, new SimpleMeterRegistry(),
                JsonMapper.builder().build(), hearts, mock(com.softlaunch.user.hearts.FreeTier.class));
        when(purchases.save(any())).thenAnswer(i -> { Purchase p = i.getArgument(0); orders.put(p.getProviderOrderId(), p); return p; });
        when(purchases.findByProviderOrderId(any())).thenAnswer(i -> Optional.ofNullable(orders.get(i.<String>getArgument(0))));
        when(memberships.findForUpdate(any())).thenAnswer(i -> Optional.ofNullable(members.get(i.<UUID>getArgument(0))));
        when(memberships.save(any())).thenAnswer(i -> { PlusMembership m = i.getArgument(0); members.put(me, m); return m; });
        when(memberships.findById(any())).thenAnswer(i -> Optional.ofNullable(members.get(i.<UUID>getArgument(0))));
    }

    @Test
    void aForgedSignatureActivatesNothing() {
        var checkout = billing.checkout(me, "plus-month");
        assertThatThrownBy(() -> billing.confirm(me, checkout.orderId(), "pay_x", "deadbeef"))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(entitlement);
        assertThat(billing.status(me).plus()).isFalse();
    }

    @Test
    void aValidPaymentActivatesPlusForThePlanPeriod() {
        var checkout = billing.checkout(me, "plus-month");
        assertThat(checkout.amountPaise()).isEqualTo(9900);
        var status = billing.mockPay(me, checkout.orderId());
        assertThat(status.plus()).isTrue();
        assertThat(status.activeUntil()).isBetween(Instant.now().plus(Duration.ofDays(29)), Instant.now().plus(Duration.ofDays(31)));
        verify(entitlement).activeUntil(eq(me), any());
    }

    @Test
    void confirmingTheSameOrderTwiceDoesNotExtendTwice() {
        var checkout = billing.checkout(me, "plus-month");
        String[] paid = provider.pay(checkout.orderId());
        Instant first = billing.confirm(me, checkout.orderId(), paid[0], paid[1]).activeUntil();
        Instant again = billing.confirm(me, checkout.orderId(), paid[0], paid[1]).activeUntil();   // e.g. webhook + browser
        assertThat(again).isEqualTo(first);
    }

    @Test
    void passesStack() {
        Instant one = billing.mockPay(me, billing.checkout(me, "plus-month").orderId()).activeUntil();
        Instant two = billing.mockPay(me, billing.checkout(me, "plus-month").orderId()).activeUntil();
        assertThat(Duration.between(one, two).toDays()).isEqualTo(30);
    }

    @Test
    void someoneElsesOrderCantBeConfirmed() {
        var checkout = billing.checkout(me, "plus-month");
        String[] paid = provider.pay(checkout.orderId());
        assertThatThrownBy(() -> billing.confirm(UUID.randomUUID(), checkout.orderId(), paid[0], paid[1]))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void unknownPlanIsRefused() {
        assertThatThrownBy(() -> billing.checkout(me, "plus-forever")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void aHeartsPackCreditsHeartsOnceAndNoPlus() {
        var checkout = billing.checkout(me, "hearts-30");
        billing.mockPay(me, checkout.orderId());
        verify(hearts).credit(eq(me), eq(30), eq("buy:hearts-30"), any());
        verify(memberships, never()).save(any());
        // the webhook for the same payment must not credit again
        Purchase p = orders.get(checkout.orderId());
        org.assertj.core.api.Assertions.assertThat(p.markPaid("again")).isFalse();
    }

    @Test
    void plusComesWithBonusHearts() {
        var checkout = billing.checkout(me, "plus-month");
        billing.mockPay(me, checkout.orderId());
        verify(hearts).credit(eq(me), eq(20), eq("plus_bonus"), any());
    }

    // ---- Razorpay webhook (the safety net when the browser closes before confirming) ----

    private BillingService withTrustedWebhooks() {
        MockPaymentProvider signed = spy(provider);
        doReturn(true).when(signed).verifyWebhook(any(), any());
        return new BillingService(props, signed, purchases, memberships, entitlement, new SimpleMeterRegistry(),
                JsonMapper.builder().build(), hearts, mock(com.softlaunch.user.hearts.FreeTier.class));
    }

    private static String captured(String orderId, long amount, String currency) {
        return """
                {"event":"payment.captured","payload":{"payment":{"entity":
                {"id":"pay_W1","order_id":"%s","amount":%d,"currency":"%s","status":"captured"}}}}""".formatted(orderId, amount, currency);
    }

    @Test
    void aSignedWebhookActivatesAPaymentTheBrowserNeverConfirmed() {
        var checkout = billing.checkout(me, "hearts-30");
        withTrustedWebhooks().webhook(captured(checkout.orderId(), 12900, "INR"), "sig");
        verify(hearts).credit(eq(me), eq(30), eq("buy:hearts-30"), any());
        assertThat(orders.get(checkout.orderId()).getStatus()).isEqualTo(Purchase.Status.PAID);
    }

    @Test
    void aWebhookForTheWrongAmountOrCurrencyCreditsNothing() {
        var checkout = billing.checkout(me, "hearts-30");
        var hooks = withTrustedWebhooks();
        hooks.webhook(captured(checkout.orderId(), 100, "INR"), "sig");
        hooks.webhook(captured(checkout.orderId(), 12900, "USD"), "sig");
        verifyNoInteractions(hearts);
        assertThat(orders.get(checkout.orderId()).getStatus()).isEqualTo(Purchase.Status.CREATED);
    }

    @Test
    void anUnsignedWebhookIsRefused() {
        var checkout = billing.checkout(me, "hearts-30");
        assertThatThrownBy(() -> billing.webhook(captured(checkout.orderId(), 12900, "INR"), "forged"))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(hearts);
    }

    @Test
    void aRepeatedWebhookCreditsOnce() {
        var checkout = billing.checkout(me, "hearts-30");
        var hooks = withTrustedWebhooks();
        hooks.webhook(captured(checkout.orderId(), 12900, "INR"), "sig");
        hooks.webhook(captured(checkout.orderId(), 12900, "INR"), "sig");   // Razorpay retries
        verify(hearts, times(1)).credit(eq(me), eq(30), eq("buy:hearts-30"), any());
    }

    @Test
    void webhooksForOtherSitesOrdersAreIgnored() {
        withTrustedWebhooks().webhook(captured("order_someone_else", 12900, "INR"), "sig");
        verifyNoInteractions(hearts, entitlement);
    }
}
