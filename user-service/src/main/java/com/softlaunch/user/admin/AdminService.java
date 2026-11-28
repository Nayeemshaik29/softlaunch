package com.softlaunch.user.admin;

import com.softlaunch.user.activity.ActivityRecorder;
import com.softlaunch.user.admin.AdminDtos.*;
import com.softlaunch.user.billing.PlusEntitlement;
import com.softlaunch.user.deletion.TokenRevocation;
import com.softlaunch.user.model.Profile;
import com.softlaunch.user.model.User;
import com.softlaunch.user.photo.PhotoLinks;
import com.softlaunch.user.repository.ProfileRepository;
import com.softlaunch.user.repository.UserRepository;
import com.softlaunch.user.session.RefreshTokenService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin dashboard data: users from Postgres + activity counters from Redis (written by every service).
 * Every call is audited. Answers 404 (not 403) to non-admins, so the admin area doesn't advertise itself.
 */
@Service
public class AdminService {

    private static final int PAGE = 25;

    private final UserRepository users;
    private final ProfileRepository profiles;
    private final AdminAuditRepository audit;
    private final StringRedisTemplate redis;
    private final PhotoLinks photoLinks;
    private final TokenRevocation revocation;
    private final RefreshTokenService sessions;
    private final JsonMapper json;

    public AdminService(UserRepository users, ProfileRepository profiles, AdminAuditRepository audit, StringRedisTemplate redis,
                        PhotoLinks photoLinks, TokenRevocation revocation, RefreshTokenService sessions, JsonMapper json) {
        this.users = users;
        this.profiles = profiles;
        this.audit = audit;
        this.redis = redis;
        this.photoLinks = photoLinks;
        this.revocation = revocation;
        this.sessions = sessions;
        this.json = json;
    }

    /** The gate for every admin endpoint. Writes the audit row. */
    @Transactional
    public User requireAdmin(UUID adminId, String action, UUID target) {
        User admin = users.findById(adminId).filter(User::isAdmin)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        audit.save(new AdminAudit(adminId, action, target));
        return admin;
    }

    // ------------------------------------------------------------------ overview

    public Overview overview(int rangeDays) {
        LocalDate today = LocalDate.now(ActivityRecorder.IST);
        Instant todayStart = today.atStartOfDay(ActivityRecorder.IST).toInstant();
        Instant rangeStart = today.minusDays(rangeDays - 1L).atStartOfDay(ActivityRecorder.IST).toInstant();

        List<String> last7 = new ArrayList<>();
        for (int i = 0; i < 7; i++) last7.add("act:day:" + today.minusDays(i));
        Set<String> weekly = redis.opsForSet().union(last7);

        List<DayCount> logins = new ArrayList<>();
        List<DayCount> signups = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            logins.add(new DayCount(d.toString(), num(redis.opsForValue().get("act:logins:" + d + ":total"))));
            Instant from = d.atStartOfDay(ActivityRecorder.IST).toInstant();
            Instant to = d.plusDays(1).atStartOfDay(ActivityRecorder.IST).toInstant();
            signups.add(new DayCount(d.toString(), users.countByCreatedAtBetween(from, to)));
        }
        Long activeToday = redis.opsForSet().size("act:day:" + today);
        return new Overview(users.count(), users.countByCreatedAtAfter(rangeStart), users.countByCreatedAtAfter(todayStart),
                activeToday == null ? 0 : activeToday, weekly == null ? 0 : weekly.size(),
                num(redis.opsForValue().get("act:logins:" + today + ":total")), logins, signups);
    }

    // ------------------------------------------------------------------ users

    /** filter: active (most recently active first, default) · new (newest accounts) · reported (most reported) · q = name search */
    public UserPage users(String filter, String q, int page) {
        int p = Math.max(0, page);
        List<UUID> ids;
        if (q != null && !q.isBlank()) {
            ids = users.findByDisplayNameContainingIgnoreCase(q.trim(), PageRequest.of(p, PAGE + 1, Sort.by("createdAt").descending()))
                    .stream().map(User::getId).toList();
            filter = "search";
        } else if ("new".equals(filter)) {
            ids = users.findAll(PageRequest.of(p, PAGE + 1, Sort.by("createdAt").descending())).stream().map(User::getId).toList();
        } else {
            String key = "reported".equals(filter) ? "act:reported" : "act:recent";
            filter = "reported".equals(filter) ? "reported" : "active";
            Set<String> raw = redis.opsForZSet().reverseRange(key, (long) p * PAGE, (long) (p + 1) * PAGE);
            ids = raw == null ? List.of() : raw.stream().map(UUID::fromString).toList();
        }
        boolean more = ids.size() > PAGE;
        return new UserPage(rows(ids.stream().limit(PAGE).toList()), filter, more);
    }

    public UserRow user(UUID id) {
        List<UserRow> r = rows(List.of(id));
        if (r.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such user");
        return r.get(0);
    }

    private List<UserRow> rows(List<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        Map<UUID, User> byId = users.findAllById(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UUID, Profile> profileById = profiles.findAllById(ids).stream().collect(Collectors.toMap(Profile::getUserId, Function.identity()));

        // logins in the last 7 days: one HMGET per day for all users at once
        List<Object> idKeys = ids.stream().map(UUID::toString).collect(Collectors.toList());
        long[] logins7 = new long[ids.size()];
        LocalDate today = LocalDate.now(ActivityRecorder.IST);
        for (int d = 0; d < 7; d++) {
            List<Object> vals = redis.opsForHash().multiGet("act:logins:" + today.minusDays(d), idKeys);
            for (int i = 0; i < ids.size(); i++) logins7[i] += num(vals.get(i));
        }

        List<UserRow> out = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            UUID id = ids.get(i);
            User u = byId.get(id);
            if (u == null) continue; // deleted meanwhile
            Profile prof = profileById.get(id);
            Map<Object, Object> c = redis.opsForHash().entries("act:user:" + id);
            Double last = redis.opsForZSet().score("act:recent", id.toString());
            String plus = redis.opsForValue().get(PlusEntitlement.PREFIX + id);
            String photo = null;
            if (prof != null && !prof.getPhotoKeys().isEmpty()) photo = photoLinks.of(List.of(prof.getPhotoKeys().get(0))).get(0);
            Integer age = u.getDateOfBirth() == null ? null : Period.between(u.getDateOfBirth(), LocalDate.now(ActivityRecorder.IST)).getYears();
            out.add(new UserRow(id, u.getDisplayName(), mask(u.getEmail()), age, prof == null ? null : prof.getCity(), photo,
                    u.isVerified(), u.isSuspended(), u.isAdmin(), u.getCreatedAt(),
                    last == null ? null : Instant.ofEpochMilli(last.longValue()), logins7[i],
                    num(c.get("swipes")), num(c.get("likes")), num(c.get("matches")), num(c.get("msgs")),
                    num(c.get("calls")), num(c.get("callSecs")) / 60, num(c.get("reports")),
                    plus == null ? null : Instant.parse(plus), u.getGoogleSub() != null, u.isPasswordSet()));
        }
        return out;
    }

    // ------------------------------------------------------------------ live feed + audit

    public List<FeedItem> feed() {
        List<String> raw = Optional.ofNullable(redis.opsForList().range("act:feed", 0, 29)).orElse(List.of());
        List<JsonNode> nodes = new ArrayList<>();
        for (String r : raw) {
            try {
                nodes.add(json.readTree(r));
            } catch (RuntimeException ignored) {
                // a malformed entry is skipped, never shown
            }
        }
        Set<UUID> ids = nodes.stream().map(n -> UUID.fromString(n.get("u").asString())).collect(Collectors.toSet());
        Map<UUID, User> byId = users.findAllById(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UUID, Profile> prof = profiles.findAllById(ids).stream().collect(Collectors.toMap(Profile::getUserId, Function.identity()));
        List<FeedItem> out = new ArrayList<>();
        for (JsonNode n : nodes) {
            UUID id = UUID.fromString(n.get("u").asString());
            User u = byId.get(id);
            if (u == null) continue;
            Profile p = prof.get(id);
            String photo = p == null || p.getPhotoKeys().isEmpty() ? null : photoLinks.of(List.of(p.getPhotoKeys().get(0))).get(0);
            out.add(new FeedItem(id, u.getDisplayName(), photo, n.get("k").asString(), Instant.parse(n.get("at").asString())));
        }
        return out;
    }

    public List<AuditItem> auditLog() {
        List<AdminAudit> rows = audit.findTop100ByOrderByAtDesc();
        Set<UUID> ids = new HashSet<>();
        rows.forEach(r -> {
            ids.add(r.getAdminId());
            if (r.getTargetUserId() != null) ids.add(r.getTargetUserId());
        });
        Map<UUID, String> names = users.findAllById(ids).stream().collect(Collectors.toMap(User::getId, User::getDisplayName));
        return rows.stream().map(r -> new AuditItem(r.getAdminId(), names.getOrDefault(r.getAdminId(), "(deleted)"), r.getAction(),
                r.getTargetUserId(), r.getTargetUserId() == null ? null : names.getOrDefault(r.getTargetUserId(), "(deleted)"), r.getAt())).toList();
    }

    // ------------------------------------------------------------------ moderation

    @Transactional
    public UserRow setSuspended(UUID adminId, UUID target, boolean suspended) {
        if (adminId.equals(target)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can't suspend yourself.");
        User u = users.findById(target).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such user"));
        if (u.isAdmin()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admins can't be suspended here.");
        u.setSuspended(suspended);
        if (suspended) {
            revocation.revokeAllTokensOf(target); // current access tokens stop at the gateway now
            sessions.deleteAllOf(target);         // and no refresh
        } else {
            redis.delete(TokenRevocation.PREFIX + target);
        }
        return user(target);
    }

    static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }

    private static long num(Object o) {
        if (o == null) return 0;
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
