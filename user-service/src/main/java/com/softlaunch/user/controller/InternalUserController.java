package com.softlaunch.user.controller;

import com.softlaunch.user.dto.PublicProfileResponse;
import com.softlaunch.user.dto.UserSummary;
import com.softlaunch.user.exception.UserNotFoundException;
import com.softlaunch.user.repository.ProfileRepository;
import com.softlaunch.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public InternalUserController(ProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{userId}")
    public UserSummary getUser(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(user -> new UserSummary(user.getId(), user.getDisplayName()))
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
    @PostMapping("/profiles")
    @Transactional(readOnly = true)
    public List<PublicProfileResponse> getProfiles(@RequestBody List<UUID> userIds) {
        if (userIds.size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max 100 ids per request");
        }
        return profileRepository.findAllById(userIds).stream()
                .map(PublicProfileResponse::from)
                .toList();
    }
}