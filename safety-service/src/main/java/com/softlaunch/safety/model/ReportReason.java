package com.softlaunch.safety.model;

public enum ReportReason {
    FAKE_PROFILE, HARASSMENT, SCAM, INAPPROPRIATE_CONTENT, UNDERAGE, THREAT_OF_VIOLENCE, OFFLINE_BEHAVIOUR, OTHER,
    /** Filed automatically by the system, never by a user. */
    SCREENSHOT_PATTERN;

    public boolean isUserSelectable() {
        return this != SCREENSHOT_PATTERN;
    }

    /** Reasons where someone may be in danger: go straight to the top of the queue. */
    public boolean isUrgent() {
        return this == UNDERAGE || this == THREAT_OF_VIOLENCE;
    }
}
