package com.softlaunch.user.hearts;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** One row per user: the ❤ balance. Every change also writes a HeartLedgerEntry (the receipt). */
@Entity
@Table(name = "heart_wallets")
public class HeartWallet {

    @Id
    private UUID userId;

    @Column(nullable = false)
    private int balance;

    /** Plus / free-tier members get one free crown a week. */
    private Instant lastFreeCrownAt;

    /** Free-tier (verified women / non-binary) members get 20 ❤ every 30 days. */
    private Instant lastMonthlyGrantAt;

    @Version
    private long version;

    protected HeartWallet() {
    }

    public HeartWallet(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() { return userId; }
    public int getBalance() { return balance; }
    public Instant getLastFreeCrownAt() { return lastFreeCrownAt; }
    public Instant getLastMonthlyGrantAt() { return lastMonthlyGrantAt; }

    void add(int delta) {
        if (balance + delta < 0) throw new IllegalStateException("negative balance");
        balance += delta;
    }

    void usedFreeCrown(Instant at) { lastFreeCrownAt = at; }
    void grantedMonthly(Instant at) { lastMonthlyGrantAt = at; }
}
