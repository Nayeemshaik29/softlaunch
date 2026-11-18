package com.softlaunch.user.deletion;

import com.softlaunch.user.exception.InvalidDeletionLinkException;
import com.softlaunch.user.model.User;
import com.softlaunch.user.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DeletionRequestServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final JavaMailSender mail = mock(JavaMailSender.class);
    private final AccountDeletionService deletion = mock(AccountDeletionService.class);
    private DeletionRequestService service;
    private User asha;
    private UUID id;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        service = new DeletionRequestService(users, redis, mail, deletion, Runnable::run,   // run "background" work inline
                new SimpleMeterRegistry(), "https://example.test/delete-account",
                Duration.ofMinutes(30), Duration.ofMinutes(10), "no-reply@example.test");
        id = UUID.randomUUID();
        asha = spy(new User("asha@test.com", "hash", "Asha", LocalDate.of(1998, 5, 10)));
        doReturn(id).when(asha).getId();
        when(users.findByEmail("asha@test.com")).thenReturn(Optional.of(asha));
        when(users.findById(id)).thenReturn(Optional.of(asha));
        when(values.setIfAbsent(startsWith(DeletionRequestService.COOLDOWN_PREFIX), any(), any(Duration.class))).thenReturn(true);
    }

    @Test
    void unknownEmailSendsNothing() {
        service.requestLink("nobody@test.com");
        verifyNoInteractions(mail);
        verify(values, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void emailIsNormalisedLikeLogin() {
        service.requestLink("  Asha@Test.COM ");
        verify(mail).send(any(SimpleMailMessage.class));
    }

    @Test
    void linkTokenIsInTheEmailButOnlyItsHashIsInRedis() {
        service.requestLink("asha@test.com");

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(sent.capture());
        Matcher m = Pattern.compile("https://example\\.test/delete-account#token=([A-Za-z0-9_-]{43})").matcher(sent.getValue().getText());
        assertThat(m.find()).as("link in the email").isTrue();
        String token = m.group(1);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(values).set(key.capture(), eq(id.toString()), eq(Duration.ofMinutes(30)));
        assertThat(key.getValue()).isEqualTo(DeletionRequestService.TOKEN_PREFIX + DeletionRequestService.sha256(token))
                .doesNotContain(token);
        assertThat(sent.getValue().getTo()).containsExactly("asha@test.com");
    }

    @Test
    void secondRequestWithinCooldownSendsNoEmail() {
        when(values.setIfAbsent(startsWith(DeletionRequestService.COOLDOWN_PREFIX), any(), any(Duration.class))).thenReturn(false);
        service.requestLink("asha@test.com");
        verifyNoInteractions(mail);
    }

    @Test
    void failedEmailClearsCooldownAndTokenSoThePersonCanRetry() {
        doThrow(new MailSendException("smtp down")).when(mail).send(any(SimpleMailMessage.class));
        service.requestLink("asha@test.com");
        verify(redis).delete(startsWith(DeletionRequestService.COOLDOWN_PREFIX));
        verify(redis).delete(startsWith(DeletionRequestService.TOKEN_PREFIX));
    }

    @Test
    void unknownOrUsedTokenDeletesNothing() {
        when(values.getAndDelete(any())).thenReturn(null);
        assertThatThrownBy(() -> service.confirm(DeletionRequestService.newToken()))
                .isInstanceOf(InvalidDeletionLinkException.class);
        verifyNoInteractions(deletion);
    }

    @Test
    void validTokenIsUsedUpAndDeletesThroughTheSameFlowAsTheApp() {
        String token = DeletionRequestService.newToken();
        when(values.getAndDelete(DeletionRequestService.TOKEN_PREFIX + DeletionRequestService.sha256(token))).thenReturn(id.toString());
        service.confirm(token);
        verify(deletion).deleteVerified(asha, "web");
    }

    @Test
    void failedDeletionGivesTheLinkBackForAFewMinutes() {
        String token = DeletionRequestService.newToken();
        String key = DeletionRequestService.TOKEN_PREFIX + DeletionRequestService.sha256(token);
        when(values.getAndDelete(key)).thenReturn(id.toString());
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE))
                .when(deletion).deleteVerified(asha, "web");

        assertThatThrownBy(() -> service.confirm(token)).isInstanceOf(ResponseStatusException.class);
        verify(values).set(key, id.toString(), Duration.ofMinutes(5));
    }

    @Test
    void tokensAreLongAndUnique() {
        String a = DeletionRequestService.newToken();
        assertThat(a).hasSize(43).isNotEqualTo(DeletionRequestService.newToken());
    }
}
