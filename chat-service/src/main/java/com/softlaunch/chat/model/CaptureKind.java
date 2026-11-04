package com.softlaunch.chat.model;

public enum CaptureKind {
    SCREENSHOT("took a screenshot"),
    SCREEN_RECORDING("started recording the screen");

    private final String systemText;

    CaptureKind(String systemText) { this.systemText = systemText; }

    /** Shown in the chat as "<name> took a screenshot"; the app adds the name. */
    public String systemText() { return systemText; }
}
