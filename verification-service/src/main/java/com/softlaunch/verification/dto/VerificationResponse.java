package com.softlaunch.verification.dto;

import com.softlaunch.verification.model.Verification;
import com.softlaunch.verification.model.VerificationMethod;
import com.softlaunch.verification.model.VerificationStatus;

import java.time.Instant;
import java.util.UUID;

/** Only the status goes back to the app: never the name, DOB or hash. */
public record VerificationResponse(UUID id, VerificationMethod method, VerificationStatus status,
                                   String failureReason, Instant createdAt) {
    public static VerificationResponse from(Verification v) {
        return new VerificationResponse(v.getId(), v.getMethod(), v.getStatus(),
                v.getFailureReason(), v.getCreatedAt());
    }
}
