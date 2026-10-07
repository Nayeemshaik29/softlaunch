package com.softlaunch.notification.controller;

import com.softlaunch.notification.dto.NotificationResponse;
import com.softlaunch.notification.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> myNotifications(@RequestHeader("X-User-Id") UUID me) {
        return notificationService.myNotifications(me);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markRead(@RequestHeader("X-User-Id") UUID me,
                                         @PathVariable UUID notificationId) {
        return notificationService.markRead(me, notificationId);
    }
}