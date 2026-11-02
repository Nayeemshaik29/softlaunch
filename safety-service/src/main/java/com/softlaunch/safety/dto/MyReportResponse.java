package com.softlaunch.safety.dto;

import com.softlaunch.safety.model.Report;
import com.softlaunch.safety.model.ReportReason;
import com.softlaunch.safety.model.ReportStatus;

import java.time.Instant;
import java.util.UUID;

/** RESOLVED and DISMISSED both show as "REVIEWED": the reporter doesn't learn the outcome. */
public record MyReportResponse(String reportId, UUID reportedUserId, ReportReason reason, String status,
                               Instant createdAt) {
    public static MyReportResponse from(Report r) {
        String shown = r.getStatus() == ReportStatus.OPEN ? "IN_REVIEW" : "REVIEWED";
        return new MyReportResponse(r.getId(), r.getReportedUserId(), r.getReason(), shown, r.getCreatedAt());
    }
}
