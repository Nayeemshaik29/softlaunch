package com.softlaunch.user.google;

import com.softlaunch.user.model.User;
import com.softlaunch.user.observability.AuthMetrics;
import com.softlaunch.user.repository.UserRepository;
import com.softlaunch.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GoogleAuthServiceTest {

    private final GoogleTokenVerifier verifier = mock(GoogleTokenVerifier.class);
    private final UserRepository users = mock(UserRepository.class);
    private final UserService userService = mock(UserService.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private GoogleAuthService service;
    private final UserService.LoginResult session = mock(UserService.LoginResult.class);

    @BeforeEach
    void setUp() {
        service = new GoogleAuthService(verifier, new GoogleProperties(List.of("web-client"), null), users, userService,
                encoder, mock(AuthMetrics.class), mock(com.softlaunch.user.activity.ActivityRecorder.class));
        when(verifier.verify("tok")).thenReturn(new GoogleIdentity("sub-1", "nisha@gmail.com", "Nisha Rao"));
        when(users.findByGoogleSub(any())).thenReturn(Optional.empty());
        when(users.findByEmail(any())).thenReturn(Optional.empty());
        when(encoder.encode(any())).thenReturn("random-hash");
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userService.issue(any())).thenReturn(session);
    }

    @Test
    void newPersonWithoutBirthdayIsAskedForIt() {
        var outcome = service.signIn(new GoogleSignInRequest("tok", null, null));
        assertThat(outcome).isInstanceOf(GoogleAuthService.NeedsBirthday.class);
        var body = ((GoogleAuthService.NeedsBirthday) outcome).body();
        assertThat(body.status()).isEqualTo("NEEDS_BIRTHDAY");
        assertThat(body.suggestedName()).isEqualTo("Nisha");
        verify(users, never()).save(any());
    }

    @Test
    void newPersonWithBirthdayGetsAPasswordlessAccount() {
        var outcome = service.signIn(new GoogleSignInRequest("tok", LocalDate.of(1999, 3, 14), null));
        assertThat(outcome).isInstanceOf(GoogleAuthService.LoggedIn.class);
        verify(users).save(argThat(u -> !u.isPasswordSet() && "sub-1".equals(u.getGoogleSub()) && "Nisha".equals(u.getDisplayName())));
    }

    @Test
    void existingEmailAccountGetsLinked() {
        User existing = new User("nisha@gmail.com", "hash", "Nisha", LocalDate.of(1999, 3, 14));
        when(users.findByEmail("nisha@gmail.com")).thenReturn(Optional.of(existing));
        service.signIn(new GoogleSignInRequest("tok", null, null));
        assertThat(existing.getGoogleSub()).isEqualTo("sub-1");
        assertThat(existing.isPasswordSet()).isTrue();
        verify(userService).issue(existing);
    }

    @Test
    void emailLinkedToADifferentGoogleAccountIsRefused() {
        User existing = new User("nisha@gmail.com", "hash", "Nisha", LocalDate.of(1999, 3, 14));
        existing.linkGoogle("other-sub");
        when(users.findByEmail("nisha@gmail.com")).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.signIn(new GoogleSignInRequest("tok", null, null))).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void cannotUnlinkWithoutAPassword() {
        UUID id = UUID.randomUUID();
        User googleOnly = User.fromGoogle("nisha@gmail.com", "h", "Nisha", LocalDate.of(1999, 3, 14), "sub-1");
        when(users.findById(id)).thenReturn(Optional.of(googleOnly));
        assertThatThrownBy(() -> service.unlink(id)).isInstanceOf(ResponseStatusException.class);
        service.setFirstPassword(id, "a-long-password");
        service.unlink(id);
        assertThat(googleOnly.getGoogleSub()).isNull();
    }

    @Test
    void switchedOffWhenNoClientId() {
        GoogleAuthService off = new GoogleAuthService(verifier, new GoogleProperties(List.of(), null), users, userService,
                encoder, mock(AuthMetrics.class), mock(com.softlaunch.user.activity.ActivityRecorder.class));
        assertThatThrownBy(() -> off.signIn(new GoogleSignInRequest("tok", null, null))).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(verifier);
    }
}
