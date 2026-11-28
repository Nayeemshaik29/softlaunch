package com.softlaunch.user.google;

import com.softlaunch.user.exception.UserNotFoundException;
import com.softlaunch.user.model.User;
import com.softlaunch.user.observability.AuthMetrics;
import com.softlaunch.user.repository.UserRepository;
import com.softlaunch.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
public class GoogleAuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Either logged in, or we need the birthday first. */
    public sealed interface Outcome permits LoggedIn, NeedsBirthday {
    }

    public record LoggedIn(UserService.LoginResult result) implements Outcome {
    }

    public record NeedsBirthday(GoogleNeedsBirthday body) implements Outcome {
    }

    private final GoogleTokenVerifier verifier;
    private final GoogleProperties props;
    private final UserRepository users;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthMetrics metrics;
    private final com.softlaunch.user.activity.ActivityRecorder activity;

    public GoogleAuthService(GoogleTokenVerifier verifier, GoogleProperties props, UserRepository users,
                             UserService userService, PasswordEncoder passwordEncoder, AuthMetrics metrics,
                             com.softlaunch.user.activity.ActivityRecorder activity) {
        this.activity = activity;
        this.verifier = verifier;
        this.props = props;
        this.users = users;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.metrics = metrics;
    }

    public boolean available() {
        return props.enabled();
    }

    @Transactional
    public Outcome signIn(GoogleSignInRequest request) {
        requireEnabled();
        GoogleIdentity id = verifier.verify(request.idToken());

        User user = users.findByGoogleSub(id.sub()).orElse(null);
        if (user == null) {
            // Same (Google-verified) email as an existing account: it's the same person, link them.
            user = users.findByEmail(id.email()).orElse(null);
            if (user != null) {
                if (user.getGoogleSub() != null) {
                    // that account is already linked to a DIFFERENT Google account
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "This email is linked to another Google account.");
                }
                user.linkGoogle(id.sub());
            }
        }
        if (user == null) {
            if (request.dateOfBirth() == null) {
                return new NeedsBirthday(new GoogleNeedsBirthday(id.email(), suggestedName(id.name())));
            }
            String name = request.displayName() != null && !request.displayName().isBlank()
                    ? request.displayName().trim() : suggestedName(id.name());
            if (name.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tell us your first name.");
            }
            user = users.save(User.fromGoogle(id.email(), passwordEncoder.encode(randomSecret()), name,
                    request.dateOfBirth(), id.sub()));
            metrics.signupCommitted();
            activity.feed(user.getId(), "signup_google");
        }
        return new LoggedIn(userService.issue(user));
    }

    @Transactional
    public void link(UUID userId, String idToken) {
        requireEnabled();
        GoogleIdentity id = verifier.verify(idToken);
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        users.findByGoogleSub(id.sub()).filter(other -> !other.getId().equals(userId)).ifPresent(other -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That Google account is linked to another profile.");
        });
        if (user.getGoogleSub() != null && !user.getGoogleSub().equals(id.sub())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unlink your current Google account first.");
        }
        user.linkGoogle(id.sub());
    }

    @Transactional
    public void unlink(UUID userId) {
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (!user.isPasswordSet()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Set a password first, so you can still log in.");
        }
        user.unlinkGoogle();
    }

    /** First password for a Google-only account. Changing an existing password is a separate, re-authenticated flow. */
    @Transactional
    public void setFirstPassword(UUID userId, String password) {
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (user.isPasswordSet()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have a password.");
        }
        user.setPassword(passwordEncoder.encode(password));
    }

    @Transactional(readOnly = true)
    public SignInMethods methods(UUID userId) {
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return new SignInMethods(user.getEmail(), user.isPasswordSet(), user.getGoogleSub() != null, props.enabled());
    }

    private void requireEnabled() {
        if (!props.enabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in isn't set up yet.");
        }
    }

    private static String suggestedName(String googleName) {
        if (googleName == null) {
            return "";
        }
        String first = googleName.trim().split("\\s+")[0];
        return first.length() > 50 ? first.substring(0, 50) : first;
    }

    private static String randomSecret() {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
