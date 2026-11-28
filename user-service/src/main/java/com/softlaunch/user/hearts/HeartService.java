package com.softlaunch.user.hearts;

import com.softlaunch.user.activity.ActivityRecorder;
import com.softlaunch.user.billing.PlusEntitlement;
import com.softlaunch.user.outbox.OutboxEvent;
import com.softlaunch.user.outbox.OutboxRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The ❤ wallet: credit (purchases, Plus bonus, free-tier monthly grant) and spend (crown, note, gift, keep chat,
 * call pass). Every change is a ledger row inside the same transaction as the balance change, under a row lock,
 * so two taps can't spend the same hearts twice.
 */
@Service
public class HeartService {

    static final Duration CROWN_FOR = Duration.ofMinutes(30);
    static final int MONTHLY_FREE = 20;

    private final HeartWalletRepository wallets;
    private final HeartLedgerRepository ledger;
    private final StringRedisTemplate redis;
    private final FreeTier freeTier;
    private final OutboxRepository outbox;

    public HeartService(HeartWalletRepository wallets, HeartLedgerRepository ledger, StringRedisTemplate redis,
                        FreeTier freeTier, OutboxRepository outbox) {
        this.wallets = wallets;
        this.ledger = ledger;
        this.redis = redis;
        this.freeTier = freeTier;
        this.outbox = outbox;
    }

    public record LedgerView(int delta, String reason, Instant at) {
    }

    public record Wallet(int balance, boolean freeTier, boolean freeCrownAvailable, Instant crownedUntil,
                         List<LedgerView> recent, java.util.Map<String, Integer> prices) {
    }

    public record Spent(int balance, HeartItem item, boolean free, Instant until) {
    }

    @Transactional
    public Wallet wallet(UUID userId) {
        HeartWallet w = lockOrCreate(userId);
        boolean free = freeTier.eligible(userId);
        Instant now = Instant.now();
        if (free && (w.getLastMonthlyGrantAt() == null || w.getLastMonthlyGrantAt().isBefore(now.minus(Duration.ofDays(30))))) {
            change(w, MONTHLY_FREE, "monthly", null);
            w.grantedMonthly(now);
        }
        return new Wallet(w.getBalance(), free, freeCrownAvailable(userId, w), crownedUntil(userId),
                ledger.findTop20ByUserIdOrderByAtDesc(userId).stream().map(e -> new LedgerView(e.getDelta(), e.getReason(), e.getAt())).toList(),
                java.util.Arrays.stream(HeartItem.values()).collect(java.util.stream.Collectors.toMap(Enum::name, i -> i.cost)));
    }

    /** Purchases and the Plus bonus. ref = purchase id: the same purchase never credits twice (caller guards). */
    @Transactional
    public int credit(UUID userId, int hearts, String reason, String ref) {
        HeartWallet w = lockOrCreate(userId);
        change(w, hearts, reason, ref);
        return w.getBalance();
    }

    /**
     * Spend for one item. ref: the match id for CALL_PASS / KEEP_CHAT, the target for NOTE / GIFT.
     * 402 Payment Required when the balance is too low (the app then opens the Hearts store).
     */
    @Transactional
    public Spent spend(UUID userId, HeartItem item, String ref) {
        HeartWallet w = lockOrCreate(userId);
        Instant now = Instant.now();
        boolean free = false;
        Instant until = null;
        switch (item) {
            case CROWN -> {
                Instant crowned = crownedUntil(userId);
                if (crowned != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "You're already wearing the crown.");
                free = freeCrownAvailable(userId, w);
                until = now.plus(CROWN_FOR);
            }
            case CALL_PASS, KEEP_CHAT -> {
                if (ref == null || ref.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Which chat?");
                UUID.fromString(ref); // must be a match id
                if (item == HeartItem.CALL_PASS) {
                    if (freeTier.eligible(userId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Calls are already free for you.");
                    if (Boolean.TRUE.equals(redis.hasKey(passKey(ref, userId)))) throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have today's pass.");
                    until = nextMidnightIst();
                }
            }
            default -> { }
        }
        if (free) {
            w.usedFreeCrown(now);
            ledger.save(new HeartLedgerEntry(userId, 0, "crown_free", null));
        } else {
            if (w.getBalance() < item.cost) {
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "You need " + item.cost + " ❤ for this. You have " + w.getBalance() + ".");
            }
            change(w, -item.cost, item.name().toLowerCase(), ref);
        }
        apply(userId, item, ref, now, until);
        return new Spent(w.getBalance(), item, free, until);
    }

    /** Account deletion: wallet + receipts go (purchases are kept separately for tax, ids and amounts only). */
    @Transactional
    public void forget(UUID userId) {
        ledger.deleteAllOf(userId);
        wallets.deleteById(userId);
        redis.delete("crown:" + userId);
        redis.opsForZSet().remove("crowns", userId.toString());
    }

    // ------------------------------------------------------------------ effects

    private void apply(UUID userId, HeartItem item, String ref, Instant now, Instant until) {
        switch (item) {
            case CROWN -> {
                redis.opsForValue().set("crown:" + userId, Long.toString(until.toEpochMilli()), CROWN_FOR);
                redis.opsForZSet().add("crowns", userId.toString(), until.toEpochMilli());   // discovery-service puts these first
            }
            case CALL_PASS -> redis.opsForValue().set(passKey(ref, userId), "1", Duration.between(now, until));
            case KEEP_CHAT -> outbox.save(OutboxEvent.of("chat.kept", ref,
                    "{\"matchId\":\"" + ref + "\",\"userId\":\"" + userId + "\",\"hours\":24}"));   // chat-service checks the user is in that chat
            default -> { }   // NOTE / GIFT: the calling service does the action
        }
    }

    private boolean freeCrownAvailable(UUID userId, HeartWallet w) {
        boolean member = Boolean.TRUE.equals(redis.hasKey(PlusEntitlement.PREFIX + userId)) || freeTier.eligible(userId);
        return member && (w.getLastFreeCrownAt() == null || w.getLastFreeCrownAt().isBefore(Instant.now().minus(Duration.ofDays(7))));
    }

    private Instant crownedUntil(UUID userId) {
        String v = redis.opsForValue().get("crown:" + userId);
        if (v == null) return null;
        Instant until = Instant.ofEpochMilli(Long.parseLong(v));
        return until.isAfter(Instant.now()) ? until : null;
    }

    static String passKey(String matchId, UUID userId) {
        return "callpass:" + matchId + ":" + userId;
    }

    static Instant nextMidnightIst() {
        return LocalDate.now(ActivityRecorder.IST).plusDays(1).atStartOfDay(ActivityRecorder.IST).toInstant();
    }

    private HeartWallet lockOrCreate(UUID userId) {
        return wallets.findForUpdate(userId).orElseGet(() -> wallets.saveAndFlush(new HeartWallet(userId)));
    }

    private void change(HeartWallet w, int delta, String reason, String ref) {
        w.add(delta);
        ledger.save(new HeartLedgerEntry(w.getUserId(), delta, reason, ref));
    }
}
