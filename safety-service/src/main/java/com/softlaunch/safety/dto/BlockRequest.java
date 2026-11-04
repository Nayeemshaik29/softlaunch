package com.softlaunch.safety.dto;

import com.softlaunch.safety.model.ReportReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** report is optional: "block AND report" files the report first, so chat evidence is copied before it's deleted. */
public record BlockRequest(@NotNull UUID blockedUserId, @Valid ReportPart report) {

    public record ReportPart(@NotNull ReportReason reason, @Size(max = 1000) String details, UUID matchId) { }
}
