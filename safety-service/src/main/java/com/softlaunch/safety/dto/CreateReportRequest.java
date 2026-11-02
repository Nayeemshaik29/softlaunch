package com.softlaunch.safety.dto;

import com.softlaunch.safety.model.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** matchId is optional: send it when reporting something that happened in a chat. */
public record CreateReportRequest(
        @NotNull UUID reportedUserId,
        @NotNull ReportReason reason,
        @Size(max = 1000) String details,
        UUID matchId) { }
