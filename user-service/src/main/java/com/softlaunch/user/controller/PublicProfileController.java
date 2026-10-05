package com.softlaunch.user.controller;

import com.softlaunch.user.dto.PublicProfileResponse;
import com.softlaunch.user.service.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PublicProfileController {

    private final ProfileService profileService;

    public PublicProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/api/users/{userId}/profile")
    public PublicProfileResponse getPublicProfile(@PathVariable UUID userId) {
        return profileService.getPublicProfile(userId);
    }
}