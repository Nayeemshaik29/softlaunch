package com.softlaunch.user.session;

import com.softlaunch.user.model.User;
import com.softlaunch.user.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private final RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final Map<String, RefreshToken> db = new HashMap<>(); // fake table, keyed by hash
    private final Instant now = Instant.parse("2026-11-18T12:00:00Z");
    private RefreshTokenService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repo, users, new SimpleMeterRegistry(), Duration.ofDays(30), Clock.fixed(now, ZoneOffset.UTC));
        User user = spy(new User("asha@test.com", "hash", "Asha", LocalDate.of(1998, 5, 10)));
        userId = UUID.randomUUID();
        doReturn(userId).when(user).getId();
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(repo.save(any())).thenAnswer(inv -> {
            RefreshToken t = inv.getArgument(0);
            var f = RefreshToken.class.getDeclaredField("tokenHash");
            f.setAccessible(true);
            db.put((String) f.get(t), t);
            return t;
        });
        when(repo.findByTokenHash(anyString())).thenAnswer(inv -> Optional.ofNullable(db.get(inv.<String>getArgument(0))));
        when(repo.revokeFamily(any(), any())).thenAnswer(inv -> {
            UUID fam = inv.getArgument(0);
            int n = 0;
            for (RefreshToken t : db.values()) if (t.getFamilyId().equals(fam) && !t.isRevoked()) { t.revoke(now); n++; }
            return n;
        });
    }

    @Test
    void onlyTheHashIsStored() {
        String raw = service.startSession(userId);
        assertThat(raw).hasSize(43);
        assertThat(db).containsKey(RefreshTokenService.hash(raw)).doesNotContainKey(raw);
    }

    @Test
    void rotationGivesANewTokenInTheSameSessionAndRetiresTheOld() {
        String first = service.startSession(userId);
        RefreshTokenService.Rotation r = service.rotate(first);
        assertThat(r.refreshToken()).isNotEqualTo(first);
        assertThat(r.user().getId()).isEqualTo(userId);
        RefreshToken old = db.get(RefreshTokenService.hash(first));
        RefreshToken fresh = db.get(RefreshTokenService.hash(r.refreshToken()));
        assertThat(old.isRevoked()).isTrue();
        assertThat(fresh.isRevoked()).isFalse();
        assertThat(fresh.getFamilyId()).isEqualTo(old.getFamilyId());
        assertThat(fresh.getExpiresAt()).isEqualTo(now.plus(Duration.ofDays(30)));
    }

    @Test
    void reusingARetiredTokenKillsTheWholeSession() {
        String first = service.startSession(userId);
        String second = service.rotate(first).refreshToken();   // owner refreshed
        assertThatThrownBy(() -> service.rotate(first))           // a copy of the old token shows up
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(db.get(RefreshTokenService.hash(second)).isRevoked()).as("the newest token dies too").isTrue();
        assertThatThrownBy(() -> service.rotate(second)).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void otherSessionsOfTheSameUserSurviveAReuse() {
        String phone = service.startSession(userId);
        String laptop = service.startSession(userId);
        service.rotate(phone);
        assertThatThrownBy(() -> service.rotate(phone)).isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(service.rotate(laptop).refreshToken()).isNotBlank();
    }

    @Test
    void unknownOrExpiredTokensAreRefused() {
        assertThatThrownBy(() -> service.rotate("made-up")).isInstanceOf(InvalidRefreshTokenException.class);
        String raw = service.startSession(userId);
        RefreshTokenService later = new RefreshTokenService(repo, users, new SimpleMeterRegistry(), Duration.ofDays(30),
                Clock.fixed(now.plus(Duration.ofDays(31)), ZoneOffset.UTC));
        assertThatThrownBy(() -> later.rotate(raw)).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logoutEndsTheSession() {
        String raw = service.startSession(userId);
        service.endSession(raw);
        ArgumentCaptor<UUID> fam = ArgumentCaptor.forClass(UUID.class);
        verify(repo).revokeFamily(fam.capture(), eq(now));
        assertThatThrownBy(() -> service.rotate(raw)).isInstanceOf(InvalidRefreshTokenException.class);
    }
}
