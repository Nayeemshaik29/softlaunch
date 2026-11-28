package com.softlaunch.user.admin;

import com.softlaunch.user.admin.AdminDtos.*;
import com.softlaunch.user.model.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** /api/admin/** (routed by the gateway). Every endpoint checks the admin flag in the database and is audited. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/me")
    public Me me(@RequestHeader("X-User-Id") UUID me) {
        User u = admin.requireAdmin(me, "open_dashboard", null);
        return new Me(u.getId(), u.getDisplayName());
    }

    @GetMapping("/overview")
    public Overview overview(@RequestHeader("X-User-Id") UUID me, @RequestParam(defaultValue = "1") int days) {
        admin.requireAdmin(me, "view_overview", null);
        return admin.overview(days == 7 || days == 30 ? days : 1);
    }

    @GetMapping("/users")
    public UserPage users(@RequestHeader("X-User-Id") UUID me, @RequestParam(defaultValue = "active") String filter,
                          @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page) {
        admin.requireAdmin(me, "list_users", null);
        return admin.users(filter, q == null ? null : q.substring(0, Math.min(q.length(), 50)), page);
    }

    @GetMapping("/users/{id}")
    public UserRow user(@RequestHeader("X-User-Id") UUID me, @PathVariable UUID id) {
        admin.requireAdmin(me, "view_user", id);
        return admin.user(id);
    }

    @PostMapping("/users/{id}/suspend")
    public UserRow suspend(@RequestHeader("X-User-Id") UUID me, @PathVariable UUID id) {
        admin.requireAdmin(me, "suspend_user", id);
        return admin.setSuspended(me, id, true);
    }

    @PostMapping("/users/{id}/unsuspend")
    public UserRow unsuspend(@RequestHeader("X-User-Id") UUID me, @PathVariable UUID id) {
        admin.requireAdmin(me, "unsuspend_user", id);
        return admin.setSuspended(me, id, false);
    }

    @GetMapping("/feed")
    public List<FeedItem> feed(@RequestHeader("X-User-Id") UUID me) {
        admin.requireAdmin(me, "view_feed", null);
        return admin.feed();
    }

    @GetMapping("/audit")
    public List<AuditItem> audit(@RequestHeader("X-User-Id") UUID me) {
        admin.requireAdmin(me, "view_audit", null);
        return admin.auditLog();
    }
}
