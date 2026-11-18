package com.softlaunch.user.deletion;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public (no login) endpoints behind the web page /delete-account.
 * Under /api/users/auth/** so the gateway treats them as public; they have their own strict rate limit there.
 */
@RestController
@RequestMapping("/api/users/auth/deletion-requests")
public class DeletionRequestController {

    private final DeletionRequestService service;

    public DeletionRequestController(DeletionRequestService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> requestLink(@Valid @RequestBody DeletionLinkRequest request) {
        service.requestLink(request.email());
        return Map.of("message", "If an account uses this email, we have sent it a link. It works for %d minutes."
                .formatted(service.linkTtl().toMinutes()));
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@Valid @RequestBody DeletionConfirmRequest request) {
        service.confirm(request.token());
    }
}
