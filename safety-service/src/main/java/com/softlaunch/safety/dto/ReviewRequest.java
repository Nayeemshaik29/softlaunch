package com.softlaunch.safety.dto;

import com.softlaunch.safety.model.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(@NotNull ReportStatus status, @Size(max = 1000) String moderatorNote) { }
