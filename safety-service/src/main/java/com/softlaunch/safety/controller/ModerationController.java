package com.softlaunch.safety.controller;

import com.softlaunch.safety.dto.ModeratorReportResponse;
import com.softlaunch.safety.dto.ReviewRequest;
import com.softlaunch.safety.dto.UserSafetyResponse;
import com.softlaunch.safety.model.ReportStatus;
import com.softlaunch.safety.service.ModerationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Moderator tools. Under /internal, so the gateway never exposes them to app users.
 * A real admin panel with roles comes later.
 */
@RestController
@RequestMapping("/internal/safety")
public class ModerationController {

    private final ModerationService moderationService;

    public ModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @GetMapping("/reports")
    public List<ModeratorReportResponse> queue(@RequestParam(defaultValue = "OPEN") ReportStatus status,
                                               @RequestParam(defaultValue = "50") int limit) {
        return moderationService.queue(status, limit);
    }

    @PatchMapping("/reports/{reportId}")
    public ModeratorReportResponse review(@PathVariable String reportId, @Valid @RequestBody ReviewRequest request) {
        return moderationService.review(reportId, request);
    }

    @GetMapping("/users/{userId}/status")
    public UserSafetyResponse userStatus(@PathVariable UUID userId) {
        return moderationService.userStatus(userId);
    }
}
