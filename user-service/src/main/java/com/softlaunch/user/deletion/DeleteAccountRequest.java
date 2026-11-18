package com.softlaunch.user.deletion;

import jakarta.validation.constraints.NotBlank;

/** The password again: someone holding an unlocked phone (or a stolen token) can't delete the account. */
public record DeleteAccountRequest(@NotBlank String password) {
}
