package com.softlaunch.user.billing;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Plus is active while now < activeUntil. Passes stack: buying again extends from the current end. */
@Entity
@Table(name = "plus_memberships")
public class PlusMembership {

    @Id
    private UUID userId;
    @Column(nullable = false)
    private Instant activeUntil;
    @Column(nullable = false, length = 40)
    private String lastPlanId;

    protected PlusMembership() {
    }

    PlusMembership(UUID userId) {
        this.userId = userId;
        this.activeUntil = Instant.EPOCH;
        this.lastPlanId = "";
    }

    public Instant getActiveUntil() { return activeUntil; }
    public String getLastPlanId() { return lastPlanId; }

    void extend(BillingProperties.Plan plan, Instant now) {
        Instant from = activeUntil.isAfter(now) ? activeUntil : now;
        activeUntil = from.plus(plan.period());
        lastPlanId = plan.id();
    }
}
