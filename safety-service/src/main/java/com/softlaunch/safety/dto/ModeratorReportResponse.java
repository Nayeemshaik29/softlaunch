package com.softlaunch.safety.dto;

import com.softlaunch.safety.model.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full view, only for moderators (internal endpoint, never routed by the gateway). */
public record ModeratorReportResponse(String reportId, UUID reporterId, UUID reportedUserId, ReportReason reason,
                                      String details, UUID matchId, EvidenceStatus evidenceStatus,
                                      List<EvidenceMessage> evidence, ReportStatus status, ReportPriority priority,
                                      String moderatorNote, Instant createdAt, Instant reviewedAt) {
    public static ModeratorReportResponse from(Report r) {
        return new ModeratorReportResponse(r.getId(), r.getReporterId(), r.getReportedUserId(), r.getReason(),
                r.getDetails(), r.getMatchId(), r.getEvidenceStatus(), r.getEvidence(), r.getStatus(),
                r.getPriority(), r.getModeratorNote(), r.getCreatedAt(), r.getReviewedAt());
    }
}
