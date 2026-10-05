package com.softlaunch.match.controller;

import com.softlaunch.match.dto.SwipeRequest;
import com.softlaunch.match.dto.SwipeResponse;
import com.softlaunch.match.service.SwipeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/matches/swipes")
public class SwipeController {

    private final SwipeService swipeService;

    public SwipeController(SwipeService swipeService) {
        this.swipeService = swipeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SwipeResponse swipe(@RequestHeader("X-User-Id") UUID swiperId,
                               @Valid @RequestBody SwipeRequest request) {
        return swipeService.swipe(swiperId, request);
    }
}