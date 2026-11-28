package com.softlaunch.user.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Admin dashboard answers. Deliberately NOT here: message text, call content, location, date of birth, ID documents. */
public final class AdminDtos {
    private AdminDtos() {
    }

    public record Me(UUID id, String displayName) {
    }

    public record DayCount(String day, long count) {
    }

    public record Overview(long totalUsers, long newUsers, long newToday, long activeToday, long active7d,
                           long loginsToday, List<DayCount> logins14d, List<DayCount> newUsers14d) {
    }

    /** One row in the users table. Email is masked (ki***@gmail.com). */
    public record UserRow(UUID id, String name, String maskedEmail, Integer age, String city, String photo,
                          boolean verified, boolean suspended, boolean admin, Instant joined, Instant lastActive,
                          long logins7d, long swipes, long likes, long matches, long messages, long calls,
                          long callMinutes, long reports, Instant plusUntil, boolean google, boolean password) {
    }

    public record UserPage(List<UserRow> users, String filter, boolean hasMore) {
    }

    public record FeedItem(UUID userId, String name, String photo, String kind, Instant at) {
    }

    public record AuditItem(UUID adminId, String adminName, String action, UUID targetUserId, String targetName, Instant at) {
    }
}
