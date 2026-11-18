package com.softlaunch.user.controller;

import com.softlaunch.user.dto.AuthResponse;
import com.softlaunch.user.dto.LoginRequest;
import com.softlaunch.user.dto.SignupRequest;
import com.softlaunch.user.dto.UserResponse;
import com.softlaunch.user.service.UserService;
import jakarta.validation.Valid;
import com.softlaunch.user.session.InvalidRefreshTokenException;
import com.softlaunch.user.session.SessionCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/auth")
public class AuthController {

    private final UserService userService;
    private final SessionCookie cookie;

    public AuthController(UserService userService, SessionCookie cookie) {
        this.userService = userService;
        this.cookie = cookie;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest request) {
        return userService.signup(request);
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        UserService.LoginResult result = userService.login(request);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.set(result.refreshToken())).body(result.response());
    }

    /** New access token from the refresh cookie (rotates the cookie too). */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = SessionCookie.NAME, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        // an invalid token throws InvalidRefreshTokenException: 401 + the cookie is cleared (GlobalExceptionHandler)
        UserService.LoginResult result = userService.refresh(refreshToken);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.set(result.refreshToken())).body(result.response());
    }

    /** Ends this session everywhere it was refreshed. Always 204, even without a cookie. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = SessionCookie.NAME, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            userService.logout(refreshToken);
        }
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.clear()).build();
    }
}