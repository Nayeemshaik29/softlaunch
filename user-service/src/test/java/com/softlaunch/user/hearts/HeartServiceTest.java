package com.softlaunch.user.hearts;

import com.softlaunch.user.outbox.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HeartServiceTest {

    private final HeartWalletRepository wallets = mock(HeartWalletRepository.class);
    private final HeartLedgerRepository ledger = mock(HeartLedgerRepository.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    @SuppressWarnings("unchecked")
    private final ZSetOperations<String, String> zsets = mock(ZSetOperations.class);
    private final FreeTier freeTier = mock(FreeTier.class);
    private final OutboxRepository outbox = mock(OutboxRepository.class);
    private final Map<UUID, HeartWallet> db = new HashMap<>();
    private final List<HeartLedgerEntry> receipts = new ArrayList<>();
    private HeartService service;
    private final UUID me = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new HeartService(wallets, ledger, redis, freeTier, outbox);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForZSet()).thenReturn(zsets);
        when(wallets.findForUpdate(any())).thenAnswer(i -> Optional.ofNullable(db.get(i.<UUID>getArgument(0))));
        when(wallets.saveAndFlush(any())).thenAnswer(i -> { HeartWallet w = i.getArgument(0); db.put(w.getUserId(), w); return w; });
        when(ledger.save(any())).thenAnswer(i -> { receipts.add(i.getArgument(0)); return i.getArgument(0); });
    }

    @Test
    void spendingNeedsEnoughHeartsAndWritesAReceipt() {
        assertThatThrownBy(() -> service.spend(me, HeartItem.CROWN, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("402");
        service.credit(me, 30, "buy:hearts-30", "p1");
        var spent = service.spend(me, HeartItem.CROWN, null);
        assertThat(spent.balance()).isEqualTo(15);
        assertThat(receipts).extracting(HeartLedgerEntry::getDelta).containsExactly(30, -15);
        verify(values).set(eq("crown:" + me), anyString(), any(java.time.Duration.class));   // crown goes live
    }

    @Test
    void verifiedWomenDontBuyCallPasses() {
        when(freeTier.eligible(me)).thenReturn(true);
        service.credit(me, 50, "x", null);
        assertThatThrownBy(() -> service.spend(me, HeartItem.CALL_PASS, UUID.randomUUID().toString()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("free");
    }

    @Test
    void aCallPassLastsUntilMidnightForThatMatchOnly() {
        service.credit(me, 10, "x", null);
        String match = UUID.randomUUID().toString();
        var spent = service.spend(me, HeartItem.CALL_PASS, match);
        assertThat(spent.until()).isEqualTo(HeartService.nextMidnightIst());
        verify(values).set(eq("callpass:" + match + ":" + me), eq("1"), any(java.time.Duration.class));
        assertThat(spent.balance()).isZero();
    }

    @Test
    void keepChatAsksChatServiceThroughTheOutbox() {
        service.credit(me, 5, "x", null);
        service.spend(me, HeartItem.KEEP_CHAT, UUID.randomUUID().toString());
        verify(outbox).save(argThat(e -> true));
    }

    @Test
    void plusMembersGetOneFreeCrownAWeek() {
        when(redis.hasKey("plus:" + me)).thenReturn(true);
        var first = service.spend(me, HeartItem.CROWN, null);
        assertThat(first.free()).isTrue();
        when(values.get("crown:" + me)).thenReturn(null);
        assertThatThrownBy(() -> service.spend(me, HeartItem.CROWN, null)).hasMessageContaining("402"); // the 2nd costs hearts
    }

    @Test
    void freeTierGetsTwentyHeartsAMonth() {
        when(freeTier.eligible(me)).thenReturn(true);
        assertThat(service.wallet(me).balance()).isEqualTo(20);
        assertThat(service.wallet(me).balance()).isEqualTo(20);   // not again within 30 days
    }
}
