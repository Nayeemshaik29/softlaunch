package com.softlaunch.user.controller;

import com.softlaunch.user.dto.OptionDto;
import com.softlaunch.user.dto.ProfileOptionsResponse;
import com.softlaunch.user.model.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
public class ProfileOptionsController {

    private static final List<String> SUGGESTED_PROMPTS = List.of(
            "My ideal first date",
            "A green flag I look for",
            "I'm weirdly attracted to",
            "The way to win me over",
            "Two truths and a lie",
            "My most controversial food opinion",
            "Best hangout spot in my city"
    );

    @GetMapping("/api/users/profile-options")
    public ProfileOptionsResponse getProfileOptions() {
        return new ProfileOptionsResponse(
                toOptions(Gender.values()),
                toOptions(RelationshipIntent.values()),
                toOptions(Interest.values()),
                toOptions(Habit.values()),
                Map.of(
                        "maxLookingFor", ProfileLimits.MAX_LOOKING_FOR,
                        "maxInterests", ProfileLimits.MAX_INTERESTS,
                        "maxHangoutPlaces", ProfileLimits.MAX_HANGOUT_PLACES,
                        "maxPrompts", ProfileLimits.MAX_PROMPTS,
                        "maxPhotos", ProfileLimits.MAX_PHOTOS,
                        "maxLanguages", ProfileLimits.MAX_LANGUAGES
                ),
                SUGGESTED_PROMPTS
        );
    }

    private static List<OptionDto> toOptions(Enum<?>[] values) {
        return Arrays.stream(values).map(OptionDto::of).toList();
    }
}