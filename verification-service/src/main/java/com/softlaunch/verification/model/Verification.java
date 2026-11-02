package com.softlaunch.verification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "verifications")
public class Verification {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status;

    @Column(name = "subject_hash")
    private String subjectHash;

    @Column(name = "verified_name")
    private String verifiedName;

    @Column(name = "verified_dob")
    private LocalDate verifiedDob;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Verification() { }   // JPA needs this

    private static Verification base(UUID userId, VerificationStatus status) {
        Verification v = new Verification();
        v.id = UUID.randomUUID();
        v.userId = userId;
        v.method = VerificationMethod.DIGILOCKER;
        v.status = status;
        v.createdAt = Instant.now();
        return v;
    }

    public static Verification verified(UUID userId, String subjectHash, String name, LocalDate dob) {
        Verification v = base(userId, VerificationStatus.VERIFIED);
        v.subjectHash = subjectHash;
        v.verifiedName = name;
        v.verifiedDob = dob;
        return v;
    }

    public static Verification failed(UUID userId, String reason) {
        Verification v = base(userId, VerificationStatus.FAILED);
        v.failureReason = reason;
        return v;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public VerificationMethod getMethod() { return method; }
    public VerificationStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
}
