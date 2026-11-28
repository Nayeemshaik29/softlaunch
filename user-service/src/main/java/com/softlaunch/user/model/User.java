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

    // ---- Sign in with Google ----
    /** Google's stable account id ("sub"). Never the email: emails can change hands. */
    @Column(unique = true)
    private String googleSub;

    /** false for Google-only accounts: their passwordHash is a random value nobody knows. */
    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean passwordSet = true;

    // ---- Admin / moderation ----
    /** Can open the admin dashboard. Only set by hand (scripts/make-admin.sh), never through the API. */
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean admin;

    /** Suspended by an admin: can't log in or refresh; current tokens are revoked. */
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean suspended;

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

    public String getGoogleSub() {
        return googleSub;
    }

    public boolean isPasswordSet() {
        return passwordSet;
    }

    public void linkGoogle(String sub) {
        this.googleSub = sub;
    }

    public void unlinkGoogle() {
        this.googleSub = null;
    }

    /** A Google-only account: the hash is of a random secret, so password login can never succeed. */
    public static User fromGoogle(String email, String randomHash, String displayName, LocalDate dateOfBirth, String googleSub) {
        User u = new User(email, randomHash, displayName, dateOfBirth);
        u.googleSub = googleSub;
        u.passwordSet = false;
        return u;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public void setPassword(String hash) {
        this.passwordHash = hash;
        this.passwordSet = true;
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