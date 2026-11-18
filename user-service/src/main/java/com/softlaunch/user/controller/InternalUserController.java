package com.softlaunch.user.controller;

import com.softlaunch.user.dto.InternalProfileResponse;
import com.softlaunch.user.dto.PublicProfileResponse;
import com.softlaunch.user.dto.UserIdentity;
import com.softlaunch.user.dto.UserSummary;
import com.softlaunch.user.exception.UserNotFoundException;
import com.softlaunch.user.repository.ProfileRepository;
import com.softlaunch.user.repository.UserRepository;
import com.softlaunch.user.photo.PhotoLinks;
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
    private final PhotoLinks photoLinks;

    public InternalUserController(ProfileRepository profileRepository, UserRepository userRepository, PhotoLinks photoLinks) {
        this.photoLinks = photoLinks;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    /** Ids of all users with a profile, in pages (ai-service backfill). Ids only, no personal data. */
    @GetMapping("/profile-ids")
    public List<UUID> profileIds(@RequestParam int page, @RequestParam(defaultValue = "100") int size) {
        return profileRepository.findAllUserIds(org.springframework.data.domain.PageRequest.of(page, Math.min(size, 500)));
    }

    @GetMapping("/{userId}")
    public UserSummary getUser(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(user -> new UserSummary(user.getId(), user.getDisplayName()))
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
    /** Used by verification-service to compare DigiLocker details with what the user signed up with. */
    @GetMapping("/{userId}/identity")
    public UserIdentity getIdentity(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(user -> new UserIdentity(user.getId(), user.getDisplayName(), user.getDateOfBirth()))
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @PostMapping("/profiles")
    @Transactional(readOnly = true)
    public List<InternalProfileResponse> getProfiles(@RequestBody List<UUID> userIds) {
        if (userIds.size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max 100 ids per request");
        }
        return profileRepository.findAllById(userIds).stream()
                .map(p -> InternalProfileResponse.from(p, photoLinks))
                .toList();
    }
}