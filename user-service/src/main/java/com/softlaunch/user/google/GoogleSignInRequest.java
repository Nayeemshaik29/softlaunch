package com.softlaunch.user.google;

import com.softlaunch.user.validation.Adult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Sign in (or up) with a Google ID token. First call: just the token. If the person is new, the answer is
 * NEEDS_BIRTHDAY (Google doesn't tell us anyone's age) and the app sends the SAME token again with the birthday.
 */
public record GoogleSignInRequest(
        @NotBlank @Size(max = 4096) String idToken,
        @Past @Adult LocalDate dateOfBirth,
        @Size(max = 50) String displayName
) {
}
