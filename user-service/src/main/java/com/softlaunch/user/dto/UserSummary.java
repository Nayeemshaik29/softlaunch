package com.softlaunch.user.dto;

import java.util.UUID;

public record UserSummary(UUID id, String displayName) {
}