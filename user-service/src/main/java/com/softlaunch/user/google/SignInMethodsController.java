package com.softlaunch.user.google;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Settings → Sign-in: link/unlink Google, set a first password. X-User-Id comes from the gateway (verified JWT). */
@RestController
@RequestMapping("/api/users/me")
public class SignInMethodsController {

    private final GoogleAuthService google;

    public SignInMethodsController(GoogleAuthService google) {
        this.google = google;
    }

    @GetMapping("/sign-in")
    public SignInMethods methods(@RequestHeader("X-User-Id") UUID userId) {
        return google.methods(userId);
    }

    @PostMapping("/google")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void link(@RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody IdTokenRequest request) {
        google.link(userId, request.idToken());
    }

    @DeleteMapping("/google")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@RequestHeader("X-User-Id") UUID userId) {
        google.unlink(userId);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPassword(@RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody SetPasswordRequest request) {
        google.setFirstPassword(userId, request.password());
    }
}
