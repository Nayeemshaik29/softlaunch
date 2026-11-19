package com.softlaunch.user.controller;

import com.softlaunch.user.dto.AuthResponse;
import com.softlaunch.user.dto.LoginRequest;
import com.softlaunch.user.dto.SignupRequest;
import com.softlaunch.user.dto.UserResponse;
import com.softlaunch.user.service.UserService;
import jakarta.validation.Valid;
import com.softlaunch.user.session.InvalidRefreshTokenException;
import com.softlaunch.user.session.NativeClients;
import jakarta.servlet.http.HttpServletRequest;
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
    private final NativeClients nativeClients;

    public AuthController(UserService userService, SessionCookie cookie, NativeClients nativeClients) {
        this.userService = userService;
        this.cookie = cookie;
        this.nativeClients = nativeClients;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest request) {
        return userService.signup(request);
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return respond(userService.login(request), http);
    }

    /** New access token from the refresh token (cookie on the web, X-Refresh-Token header in the apps). Rotates it. */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = SessionCookie.NAME, required = false) String cookieToken,
                                                @RequestHeader(value = NativeClients.TOKEN_HEADER, required = false) String headerToken,
                                                HttpServletRequest http) {
        String refreshToken = pick(cookieToken, headerToken, http);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        // an invalid token throws InvalidRefreshTokenException: 401 + the cookie is cleared (GlobalExceptionHandler)
        return respond(userService.refresh(refreshToken), http);
    }

    /** Ends this session everywhere it was refreshed. Always 204, even without a token. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = SessionCookie.NAME, required = false) String cookieToken,
                                       @RequestHeader(value = NativeClients.TOKEN_HEADER, required = false) String headerToken,
                                       HttpServletRequest http) {
        String refreshToken = pick(cookieToken, headerToken, http);
        if (refreshToken != null && !refreshToken.isBlank()) {
            userService.logout(refreshToken);
        }
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.clear()).build();
    }

    /** Web: cookie only. Apps: the header, and only from an app origin. */
    private String pick(String cookieToken, String headerToken, HttpServletRequest http) {
        return nativeClients.wantsBody(http) ? headerToken : cookieToken;
    }

    private ResponseEntity<AuthResponse> respond(UserService.LoginResult result, HttpServletRequest http) {
        if (nativeClients.wantsBody(http)) {
            return ResponseEntity.ok().body(result.response().withRefreshToken(result.refreshToken()));
        }
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.set(result.refreshToken())).body(result.response());
    }
}
