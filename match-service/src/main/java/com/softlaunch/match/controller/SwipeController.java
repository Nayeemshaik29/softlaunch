package com.softlaunch.match.controller;

import com.softlaunch.match.dto.SwipeQuota;
import com.softlaunch.match.dto.SwipeRequest;
import com.softlaunch.match.dto.SwipeResponse;
import com.softlaunch.match.service.SwipeQuotaService;
import com.softlaunch.match.service.SwipeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/matches/swipes")
public class SwipeController {

    private final SwipeService swipeService;
    private final SwipeQuotaService quotaService;

    public SwipeController(SwipeService swipeService, SwipeQuotaService quotaService) {
        this.swipeService = swipeService;
        this.quotaService = quotaService;
    }

    /** Energy meter: swipes left today and when they refill. */
    @GetMapping("/quota")
    public SwipeQuota quota(@RequestHeader("X-User-Id") UUID userId) {
        return quotaService.status(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SwipeResponse swipe(@RequestHeader("X-User-Id") UUID swiperId,
                               @Valid @RequestBody SwipeRequest request) {
        return swipeService.swipe(swiperId, request);
    }
}