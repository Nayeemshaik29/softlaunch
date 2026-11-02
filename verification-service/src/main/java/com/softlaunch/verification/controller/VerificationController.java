package com.softlaunch.verification.controller;

import com.softlaunch.verification.config.DigiLockerProperties;
import com.softlaunch.verification.dto.StartResponse;
import com.softlaunch.verification.dto.VerificationResponse;
import com.softlaunch.verification.model.VerificationStatus;
import com.softlaunch.verification.service.VerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/verification")
public class VerificationController {

    private final VerificationService service;
    private final DigiLockerProperties props;

    public VerificationController(VerificationService service, DigiLockerProperties props) {
        this.service = service;
        this.props = props;
    }

    @PostMapping("/digilocker/start")
    public StartResponse start(@RequestHeader("X-User-Id") UUID userId) {
        return service.start(userId);
    }

    /** PUBLIC: DigiLocker redirects the user's browser here. The user is identified by the state, not a JWT. */
    @GetMapping("/digilocker/callback")
    public ResponseEntity<Void> callback(@RequestParam String state,
                                         @RequestParam(required = false) String code,
                                         @RequestParam(required = false) String error) {
        VerificationStatus status = service.complete(state, code, error);
        URI backToApp = UriComponentsBuilder.fromUriString(props.appReturnUrl())
                .queryParam("status", status)
                .build().toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(backToApp).build();
    }

    @GetMapping("/me")
    public VerificationResponse me(@RequestHeader("X-User-Id") UUID userId) {
        return service.latest(userId);
    }
}
