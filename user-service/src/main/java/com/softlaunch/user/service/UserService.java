package com.softlaunch.user.service;

import com.softlaunch.user.dto.AuthResponse;
import com.softlaunch.user.dto.LoginRequest;
import com.softlaunch.user.dto.SignupRequest;
import com.softlaunch.user.dto.UserResponse;
import com.softlaunch.user.exception.InvalidCredentialsException;
import com.softlaunch.user.exception.TooManyLoginAttemptsException;
import com.softlaunch.user.exception.UserNotFoundException;
import com.softlaunch.user.model.User;
import com.softlaunch.user.observability.AuthMetrics;
import com.softlaunch.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyHash;
    private final AuthMetrics authMetrics;
    private final LoginAttemptService loginAttempts;
    private final com.softlaunch.user.session.RefreshTokenService refreshTokens;
    private final com.softlaunch.user.activity.ActivityRecorder activity;
    private final com.softlaunch.user.hearts.FreeTier freeTier;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       AuthMetrics authMetrics, LoginAttemptService loginAttempts,
                       com.softlaunch.user.session.RefreshTokenService refreshTokens,
                       com.softlaunch.user.activity.ActivityRecorder activity,
                       com.softlaunch.user.hearts.FreeTier freeTier) {
        this.freeTier = freeTier;
        this.activity = activity;
        this.refreshTokens = refreshTokens;
        this.authMetrics = authMetrics;
        this.loginAttempts = loginAttempts;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public UserResponse signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName(),
                request.dateOfBirth()
        );

        UserResponse saved = UserResponse.from(userRepository.save(user));
        authMetrics.signupCommitted();
        activity.feed(saved.id(), "signup_email");
        return saved;
    }
    /** Access token for the response body + refresh token for the HttpOnly cookie. */
    public record LoginResult(AuthResponse response, String refreshToken) {
    }

    public LoginResult login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        // Checked BEFORE the password: while locked even the right password is refused,
        // otherwise an attacker could keep guessing and notice the one answer that differs.
        if (loginAttempts.isLocked(email)) {
            authMetrics.loginLocked();
            throw new TooManyLoginAttemptsException();
        }

        User user = userRepository.findByEmail(email).orElse(null);
        String hashToCheck = (user != null) ? user.getPasswordHash() : dummyHash;
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (user == null || !passwordMatches) {
            authMetrics.loginFailed();
            loginAttempts.recordFailure(email);
            throw new InvalidCredentialsException();
        }

        loginAttempts.recordSuccess(email);
        return issue(user);
    }

    /** A new session for someone who just proved who they are (password or Google). */
    public LoginResult issue(User user) {
        if (user.isSuspended()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is suspended. Contact support if you think that's a mistake.");
        }
        activity.login(user.getId());
        freeTier.sync(user.getId());   // verified women / non-binary: Plus is free
        String token = jwtService.generateToken(user);
        authMetrics.loginSucceeded();
        return new LoginResult(new AuthResponse(token, "Bearer", jwtService.getExpiration().toSeconds()),
                refreshTokens.startSession(user.getId()));
    }

    /** Refresh: new access token + rotated refresh token (same session). */
    public LoginResult refresh(String refreshToken) {
        com.softlaunch.user.session.RefreshTokenService.Rotation r = refreshTokens.rotate(refreshToken);
        if (r.user().isSuspended()) {
            refreshTokens.endSession(r.refreshToken());
            throw new com.softlaunch.user.session.InvalidRefreshTokenException();
        }
        return new LoginResult(new AuthResponse(jwtService.generateToken(r.user()), "Bearer", jwtService.getExpiration().toSeconds()),
                r.refreshToken());
    }

    public void logout(String refreshToken) {
        refreshTokens.endSession(refreshToken);
    }
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(id));
    }


}