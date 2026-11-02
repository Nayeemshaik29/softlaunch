package com.softlaunch.safety.dto;

/** What the REPORTER sees. Never the priority, never what happens to the other person. */
public record ReportReceipt(String reportId, String status, String message) {
    public static ReportReceipt received(String id) {
        return new ReportReceipt(id, "RECEIVED", "Thanks for letting us know. Our safety team will review this.");
    }
}
