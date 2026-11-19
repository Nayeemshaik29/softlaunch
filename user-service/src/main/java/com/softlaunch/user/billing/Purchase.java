package com.softlaunch.user.billing;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** One checkout. Kept after account deletion (tax / refund records), see docs/DATA_DELETION.md. */
@Entity
@Table(name = "purchases", indexes = @Index(name = "idx_purchase_user", columnList = "userId"))
public class Purchase {

    public enum Status { CREATED, PAID }

    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false, length = 40)
    private String planId;
    @Column(nullable = false)
    private long amountPaise;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(nullable = false, length = 20)
    private String provider;
    @Column(unique = true, length = 80)
    private String providerOrderId;
    @Column(length = 80)
    private String providerPaymentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant paidAt;

    protected Purchase() {
    }

    Purchase(UUID userId, BillingProperties.Plan plan, String currency, String provider) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.planId = plan.id();
        this.amountPaise = plan.amountPaise();
        this.currency = currency;
        this.provider = provider;
        this.status = Status.CREATED;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getPlanId() { return planId; }
    public long getAmountPaise() { return amountPaise; }
    public String getCurrency() { return currency; }
    public String getProviderOrderId() { return providerOrderId; }
    public Status getStatus() { return status; }

    void setProviderOrderId(String id) { this.providerOrderId = id; }

    /** Idempotent: a second confirmation (browser + webhook) changes nothing. */
    boolean markPaid(String paymentId) {
        if (status == Status.PAID) return false;
        status = Status.PAID;
        providerPaymentId = paymentId;
        paidAt = Instant.now();
        return true;
    }
}
