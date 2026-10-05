package com.softlaunch.user.controller;

import com.softlaunch.user.dto.MyProfileResponse;
import com.softlaunch.user.dto.ProfileRequest;
import com.softlaunch.user.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public MyProfileResponse getMyProfile(@RequestHeader("X-User-Id") UUID userId) {
        return profileService.getMyProfile(userId);
    }

    @PutMapping
    public MyProfileResponse upsertMyProfile(@RequestHeader("X-User-Id") UUID userId,
                                             @Valid @RequestBody ProfileRequest request) {
        return profileService.upsertMyProfile(userId, request);
    }
}