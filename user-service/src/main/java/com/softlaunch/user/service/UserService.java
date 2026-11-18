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

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       AuthMetrics authMetrics, LoginAttemptService loginAttempts,
                       com.softlaunch.user.session.RefreshTokenService refreshTokens) {
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

        String token = jwtService.generateToken(user);
        authMetrics.loginSucceeded();
        loginAttempts.recordSuccess(email);
        return new LoginResult(new AuthResponse(token, "Bearer", jwtService.getExpiration().toSeconds()),
                refreshTokens.startSession(user.getId()));
    }

    /** Refresh: new access token + rotated refresh token (same session). */
    public LoginResult refresh(String refreshToken) {
        com.softlaunch.user.session.RefreshTokenService.Rotation r = refreshTokens.rotate(refreshToken);
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