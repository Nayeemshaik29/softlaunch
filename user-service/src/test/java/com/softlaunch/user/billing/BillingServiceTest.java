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
            List.of(new BillingProperties.Plan("plus-month", "Plus · 1 month", 9900, Duration.ofDays(30))), null);
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
                JsonMapper.builder().build());
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
}
