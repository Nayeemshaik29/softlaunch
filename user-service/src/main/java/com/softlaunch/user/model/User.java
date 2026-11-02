package com.softlaunch.user.model;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String displayName;

    private LocalDate dateOfBirth;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // ---- Verification (set only by the user.verified event) ----
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean verified;

    private Instant verifiedAt;

    protected User() {
    }

    public User(String email, String passwordHash, String displayName, LocalDate dateOfBirth) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.dateOfBirth = dateOfBirth;
        this.createdAt = Instant.now();
    }
    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isVerified() {
        return verified;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    /** @return false if already verified (duplicate event), so callers can skip side effects. */
    public boolean markVerified(Instant at) {
        if (verified) {
            return false;
        }
        this.verified = true;
        this.verifiedAt = at;
        return true;
    }
}