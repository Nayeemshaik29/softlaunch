package com.softlaunch.user.deletion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeletionLinkRequest(@NotBlank @Email @Size(max = 254) String email) {
}
