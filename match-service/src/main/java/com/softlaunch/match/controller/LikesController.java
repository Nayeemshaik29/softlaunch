package com.softlaunch.match.controller;

import com.softlaunch.match.service.LikesService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class LikesController {

    private final LikesService likes;

    public LikesController(LikesService likes) {
        this.likes = likes;
    }

    @GetMapping("/likes")
    public LikesService.Likes likes(@RequestHeader("X-User-Id") UUID me) {
        return likes.likesOf(me);
    }

    @PostMapping("/swipes/rewind")
    public LikesService.Rewound rewind(@RequestHeader("X-User-Id") UUID me) {
        return likes.rewind(me);
    }
}
