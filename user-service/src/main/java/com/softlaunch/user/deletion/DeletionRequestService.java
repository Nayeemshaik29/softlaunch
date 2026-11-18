package com.softlaunch.user.deletion;

import com.softlaunch.user.exception.InvalidDeletionLinkException;
import com.softlaunch.user.model.User;
import com.softlaunch.user.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Account deletion from the WEB page, for people who uninstalled the app (Play Store rule).
 * They may not remember their password, so owning the email inbox is the proof instead:
 *
 *  1. requestLink(email): same answer and same speed whether or not an account exists, so the page
 *     can't be used to find out who uses a dating app. All the real work runs in the background.
 *  2. The email holds a one-time link. Redis keeps only the SHA-256 of the token: someone who can
 *     read Redis still can't build a working link.
 *  3. confirm(token): GETDEL, so the link works exactly once, then the same deletion as the app.
 */
@Service
public class DeletionRequestService {

    static final String TOKEN_PREFIX = "deletion:token:";
    static final String COOLDOWN_PREFIX = "deletion:sent:";
    private static final Duration RETRY_WINDOW = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(DeletionRequestService.class);

    private final UserRepository users;
    private final StringRedisTemplate redis;
    private final JavaMailSender mail;
    private final AccountDeletionService deletion;
    private final Executor background;
    private final MeterRegistry metrics;
    private final String pageUrl;
    private final Duration linkTtl;
    private final Duration emailCooldown;
    private final String mailFrom;

    public DeletionRequestService(UserRepository users, StringRedisTemplate redis, JavaMailSender mail,
                                  AccountDeletionService deletion,
                                  @Qualifier(DeletionConfig.EMAIL_EXECUTOR) Executor background,
                                  MeterRegistry metrics,
                                  @Value("${softlaunch.account-deletion.page-url}") String pageUrl,
                                  @Value("${softlaunch.account-deletion.link-ttl:30m}") Duration linkTtl,
                                  @Value("${softlaunch.account-deletion.email-cooldown:10m}") Duration emailCooldown,
                                  @Value("${softlaunch.account-deletion.mail-from}") String mailFrom) {
        this.users = users;
        this.redis = redis;
        this.mail = mail;
        this.deletion = deletion;
        this.background = background;
        this.metrics = metrics;
        this.pageUrl = pageUrl;
        this.linkTtl = linkTtl;
        this.emailCooldown = emailCooldown;
        this.mailFrom = mailFrom;
    }

    public Duration linkTtl() {
        return linkTtl;
    }

    /** Returns at once. Looking up the account and sending the email happen in the background. */
    public void requestLink(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        try {
            background.execute(() -> sendLinkIfAccountExists(email));
        } catch (RejectedExecutionException e) {
            log.warn("Deletion email queue is full; request dropped");
            count("dropped");
        }
    }

    void sendLinkIfAccountExists(String email) {
        Optional<User> found = users.findByEmail(email);
        if (found.isEmpty()) {
            count("no_account");
            return;
        }
        String cooldownKey = COOLDOWN_PREFIX + sha256(email);
        String tokenKey = null;
        try {
            // One email per account per 10 min: stops anyone from flooding a person's inbox through this page
            if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(cooldownKey, "1", emailCooldown))) {
                count("cooldown");
                return;
            }
            String token = newToken();
            tokenKey = TOKEN_PREFIX + sha256(token);
            redis.opsForValue().set(tokenKey, found.get().getId().toString(), linkTtl);
            mail.send(linkEmail(email, token));
            count("sent");
            log.info("Deletion link sent for account {}", found.get().getId());   // id only: never the email or token
        } catch (RuntimeException e) {
            log.warn("Deletion link NOT sent for account {} ({})", found.get().getId(), e.getClass().getSimpleName());
            count("failed");
            undo(cooldownKey, tokenKey);   // so the person can simply try again
        }
    }

    /** Called when the person presses "Delete my account" on the page. Works once per link. */
    public void confirm(String token) {
        String tokenKey = TOKEN_PREFIX + sha256(token);
        String userId;
        try {
            userId = redis.opsForValue().getAndDelete(tokenKey);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Please try again in a minute");
        }
        if (userId == null) {
            throw new InvalidDeletionLinkException();
        }
        User user = users.findById(UUID.fromString(userId)).orElseThrow(InvalidDeletionLinkException::new);
        try {
            deletion.deleteVerified(user, "web");
        } catch (RuntimeException e) {
            // Nothing was deleted (it fails before the transaction, or the transaction rolled back):
            // give the link back for a few minutes so "try again" works without a new email.
            try {
                redis.opsForValue().set(tokenKey, userId, RETRY_WINDOW);
            } catch (DataAccessException ignored) {
                // Redis is the reason it failed: they will need a new link
            }
            throw e;
        }
    }

    private SimpleMailMessage linkEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(to);
        message.setSubject("Confirm deleting your SoftLaunch account");
        // The token goes after "#": browsers never send that part to any server, so it can't end up
        // in access logs, proxies or Referer headers. Only the page's own script reads it.
        message.setText("""
                Someone (hopefully you) asked to delete the SoftLaunch account that uses this email address.

                To delete it, open this link and press "Delete my account":
                %s#token=%s

                The link works once, for %d minutes.

                If you didn't ask for this, ignore this email. Nothing happens and your account stays as it is.
                """.formatted(pageUrl, token, linkTtl.toMinutes()));
        return message;
    }

    private void undo(String cooldownKey, String tokenKey) {
        try {
            redis.delete(cooldownKey);
            if (tokenKey != null) {
                redis.delete(tokenKey);
            }
        } catch (DataAccessException ignored) {
            // Redis down: the keys expire on their own
        }
    }

    private void count(String result) {
        metrics.counter("softlaunch.deletion.links", "result", result).increment();
    }

    /** 32 random bytes = 256 bits: can't be guessed, and URL-safe without escaping. */
    static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
