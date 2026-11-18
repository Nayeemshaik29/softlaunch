package com.softlaunch.user.deletion;

import com.softlaunch.user.exception.InvalidCredentialsException;
import com.softlaunch.user.exception.TooManyLoginAttemptsException;
import com.softlaunch.user.exception.UserNotFoundException;
import com.softlaunch.user.model.User;
import com.softlaunch.user.outbox.OutboxEvent;
import com.softlaunch.user.outbox.OutboxRepository;
import com.softlaunch.user.photo.PhotoUploadRepository;
import com.softlaunch.user.repository.ProfileRepository;
import com.softlaunch.user.repository.UserRepository;
import com.softlaunch.user.service.LoginAttemptService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

/**
 * "Delete my account" (Play Store requirement). Order matters:
 *  1. password check (same lockout as login: no guessing through this door)
 *  2. revoke tokens in Redis FIRST: if Redis is down we stop here and nothing is half-deleted
 *  3. ONE transaction: delete profile + uploads + user AND write user.deleted to the outbox.
 *     Either all of it happens or none of it. The outbox publisher sends the event to Kafka,
 *     retrying until Kafka takes it, so no service can miss the deletion.
 *  4. Every service (this one too, for the photos in S3) cleans up its own data from that event.
 */
@Service
public class AccountDeletionService {

    public static final String TOPIC = "user.deleted";
    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PhotoUploadRepository uploads;
    private final OutboxRepository outbox;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;
    private final TokenRevocation tokens;
    private final TransactionTemplate tx;
    private final JsonMapper json;
    private final MeterRegistry metrics;

    public AccountDeletionService(UserRepository users, ProfileRepository profiles, PhotoUploadRepository uploads,
                                  OutboxRepository outbox, PasswordEncoder passwordEncoder,
                                  LoginAttemptService loginAttempts, TokenRevocation tokens,
                                  TransactionTemplate tx, JsonMapper json, MeterRegistry metrics) {
        this.users = users;
        this.profiles = profiles;
        this.uploads = uploads;
        this.outbox = outbox;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempts = loginAttempts;
        this.tokens = tokens;
        this.tx = tx;
        this.json = json;
        this.metrics = metrics;
    }

    public void deleteAccount(UUID userId, String password) {
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        if (loginAttempts.isLocked(user.getEmail())) {
            throw new TooManyLoginAttemptsException();
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {   // BCrypt is slow: outside the transaction
            loginAttempts.recordFailure(user.getEmail());
            metrics.counter("softlaunch.accounts.deleted", "result", "wrong_password").increment();
            throw new InvalidCredentialsException();
        }

        try {
            tokens.revokeAllTokensOf(userId);
        } catch (RuntimeException e) {
            log.warn("Could not revoke tokens of {} ({}); account NOT deleted", userId, e.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Please try again in a minute");
        }

        UserDeletedEvent event = UserDeletedEvent.of(userId);
        tx.executeWithoutResult(status -> {
            profiles.findById(userId).ifPresent(profiles::delete);   // profile shares the user's id (@MapsId): first
            uploads.deleteByUserId(userId);
            users.delete(user);
            outbox.save(OutboxEvent.of(TOPIC, userId.toString(), json.writeValueAsString(event)));
        });
        loginAttempts.recordSuccess(user.getEmail());   // clear any failure counter for that email
        metrics.counter("softlaunch.accounts.deleted", "result", "deleted").increment();
        log.info("Account {} deleted; user.deleted {} queued", userId, event.eventId());   // ids only, never the email
    }
}
