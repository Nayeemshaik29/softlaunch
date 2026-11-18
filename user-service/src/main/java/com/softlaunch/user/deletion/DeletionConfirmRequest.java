package com.softlaunch.user.deletion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Tokens are 43 URL-safe base64 characters (see DeletionRequestService.newToken). */
public record DeletionConfirmRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token) {
}
