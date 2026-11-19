package com.softlaunch.user.billing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class BillingController {

    private final BillingService billing;

    public BillingController(BillingService billing) {
        this.billing = billing;
    }

    public record CheckoutRequest(@NotBlank @Size(max = 40) String planId) {
    }

    public record ConfirmRequest(@NotBlank @Size(max = 80) String orderId, @NotBlank @Size(max = 80) String paymentId,
                                 @NotBlank @Size(max = 128) String signature) {
    }

    public record MockPayRequest(@NotBlank @Size(max = 80) String orderId) {
    }

    @GetMapping("/api/users/billing/plans")
    public List<BillingService.PlanView> plans() {
        return billing.plans();
    }

    @GetMapping("/api/users/me/billing")
    public BillingService.Status status(@RequestHeader("X-User-Id") UUID me) {
        return billing.status(me);
    }

    @PostMapping("/api/users/me/billing/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public BillingService.Checkout checkout(@RequestHeader("X-User-Id") UUID me, @Valid @RequestBody CheckoutRequest r) {
        return billing.checkout(me, r.planId());
    }

    @PostMapping("/api/users/me/billing/confirm")
    public BillingService.Status confirm(@RequestHeader("X-User-Id") UUID me, @Valid @RequestBody ConfirmRequest r) {
        return billing.confirm(me, r.orderId(), r.paymentId(), r.signature());
    }

    /** 404 unless the mock provider is active (never in production). */
    @PostMapping("/api/users/me/billing/mock-pay")
    public BillingService.Status mockPay(@RequestHeader("X-User-Id") UUID me, @Valid @RequestBody MockPayRequest r) {
        return billing.mockPay(me, r.orderId());
    }

    /** Public (no login): Razorpay calls it. Trusted only through the signature. */
    @PostMapping("/api/users/billing/webhook/razorpay")
    public void razorpayWebhook(@RequestBody String rawBody,
                                @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        billing.webhook(rawBody, signature);
    }
}
