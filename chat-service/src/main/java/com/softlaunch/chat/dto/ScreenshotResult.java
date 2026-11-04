package com.softlaunch.chat.dto;

/** ALERTED = the other person was told. DEBOUNCED = we already alerted in the last few seconds. */
public record ScreenshotResult(String status) {
    public static final ScreenshotResult ALERTED = new ScreenshotResult("ALERTED");
    public static final ScreenshotResult DEBOUNCED = new ScreenshotResult("DEBOUNCED");
}
