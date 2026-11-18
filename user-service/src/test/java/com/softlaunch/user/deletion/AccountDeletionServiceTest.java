package com.softlaunch.user.deletion;

import com.softlaunch.user.exception.InvalidCredentialsException;
import com.softlaunch.user.exception.TooManyLoginAttemptsException;
import com.softlaunch.user.model.User;
import com.softlaunch.user.outbox.OutboxEvent;
import com.softlaunch.user.outbox.OutboxRepository;
import com.softlaunch.user.photo.PhotoUploadRepository;
import com.softlaunch.user.repository.ProfileRepository;
import com.softlaunch.user.repository.UserRepository;
import com.softlaunch.user.service.LoginAttemptService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** The rule that matters most: when anything goes wrong BEFORE the delete, nothing is deleted. */
class AccountDeletionServiceTest {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(4);   // fast for tests

    private final UserRepository users = mock(UserRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final PhotoUploadRepository uploads = mock(PhotoUploadRepository.class);
    private final OutboxRepository outbox = mock(OutboxRepository.class);
    private final LoginAttemptService attempts = mock(LoginAttemptService.class);
    private final TokenRevocation tokens = mock(TokenRevocation.class);
    private final TransactionTemplate tx = mock(TransactionTemplate.class);
    private final com.softlaunch.user.session.RefreshTokenService sessions = mock(com.softlaunch.user.session.RefreshTokenService.class);
    private AccountDeletionService service;
    private User user;
    private UUID id;

    @BeforeEach
    void setUp() {
        service = new AccountDeletionService(users, profiles, uploads, outbox, ENCODER, attempts, tokens, tx,
                JsonMapper.builder().findAndAddModules().build(), new SimpleMeterRegistry(), sessions);
        user = spy(new User("asha@test.com", ENCODER.encode("right-password"), "Asha", LocalDate.of(1998, 5, 10)));
        id = UUID.randomUUID();
        doReturn(id).when(user).getId();
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(profiles.findById(id)).thenReturn(Optional.empty());
        doAnswer(inv -> { inv.<java.util.function.Consumer<Object>>getArgument(0).accept(null); return null; })
                .when(tx).executeWithoutResult(any());
    }

    @Test
    void wrongPasswordDeletesNothingAndCountsAsAFailedLogin() {
        assertThatThrownBy(() -> service.deleteAccount(id, "guess")).isInstanceOf(InvalidCredentialsException.class);
        verify(attempts).recordFailure("asha@test.com");
        verifyNoInteractions(tokens, tx, outbox, sessions);
        verify(users, never()).delete(any());
    }

    @Test
    void lockedAccountCannotBeDeletedByGuessing() {
        when(attempts.isLocked("asha@test.com")).thenReturn(true);
        assertThatThrownBy(() -> service.deleteAccount(id, "right-password")).isInstanceOf(TooManyLoginAttemptsException.class);
        verifyNoInteractions(tokens, tx);
    }

    @Test
    void redisDownMeansNothingIsDeleted() {
        doThrow(new QueryTimeoutException("redis down")).when(tokens).revokeAllTokensOf(id);
        assertThatThrownBy(() -> service.deleteAccount(id, "right-password"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("503");
        verifyNoInteractions(tx, outbox);
    }

    @Test
    void successRevokesTokensFirstThenDeletesAndQueuesTheEventTogether() {
        service.deleteAccount(id, "right-password");

        var order = inOrder(tokens, tx);
        order.verify(tokens).revokeAllTokensOf(id);
        order.verify(tx).executeWithoutResult(any());
        verify(uploads).deleteByUserId(id);
        verify(sessions).deleteAllOf(id);   // no way back in with an old refresh cookie
        verify(users).delete(user);

        ArgumentCaptor<OutboxEvent> event = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outbox).save(event.capture());
        assertThat(event.getValue().getTopic()).isEqualTo("user.deleted");
        assertThat(event.getValue().getMessageKey()).isEqualTo(id.toString());
        assertThat(event.getValue().getPayload()).contains(id.toString()).doesNotContain("asha@test.com");
    }
}
