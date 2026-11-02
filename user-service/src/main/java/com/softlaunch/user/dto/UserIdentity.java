package com.softlaunch.user.dto;

import java.time.LocalDate;
import java.util.UUID;

/** Internal only (never routed by the gateway): the details verification-service checks against DigiLocker. */
public record UserIdentity(UUID userId, String displayName, LocalDate dateOfBirth) {
}
