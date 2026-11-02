package com.softlaunch.verification.client;

import java.time.LocalDate;
import java.util.UUID;

public record UserIdentity(UUID userId, String displayName, LocalDate dateOfBirth) { }
