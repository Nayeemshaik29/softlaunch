package com.softlaunch.user.hearts;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** A receipt: +30 "buy:hearts-30", -15 "crown", +20 "monthly". ref = purchase / match id (never content). */
@Entity
@Table(name = "heart_ledger", indexes = @Index(name = "idx_heart_ledger_user", columnList = "userId, at"))
public class HeartLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int delta;

    @Column(nullable = false, length = 40)
    private String reason;

    @Column(length = 80)
    private String ref;

    @Column(nullable = false)
    private Instant at;

    protected HeartLedgerEntry() {
    }

    public HeartLedgerEntry(UUID userId, int delta, String reason, String ref) {
        this.userId = userId;
        this.delta = delta;
        this.reason = reason;
        this.ref = ref;
        this.at = Instant.now();
    }

    public int getDelta() { return delta; }
    public String getReason() { return reason; }
    public String getRef() { return ref; }
    public Instant getAt() { return at; }
}
