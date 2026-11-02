package com.softlaunch.safety.dto;

import java.time.Instant;
import java.util.UUID;

public record UserSafetyResponse(UUID userId, boolean underReview, String reviewReason, Instant underReviewSince,
                                 long openReports, long distinctReportersInWindow) { }
