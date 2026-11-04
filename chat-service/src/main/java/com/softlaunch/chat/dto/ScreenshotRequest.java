package com.softlaunch.chat.dto;

import com.softlaunch.chat.model.CaptureKind;
import jakarta.validation.constraints.NotNull;

public record ScreenshotRequest(@NotNull CaptureKind kind) {
}
