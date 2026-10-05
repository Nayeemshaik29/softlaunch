package com.softlaunch.user.dto;

import com.softlaunch.user.model.Gender;
import com.softlaunch.user.model.Habit;
import com.softlaunch.user.model.Interest;
import com.softlaunch.user.model.RelationshipIntent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Set;

public record ProfileRequest(
        @Size(max = 500) String bio,
        @Size(max = 30) String pronouns,

        @NotNull Gender gender,
        @NotEmpty Set<Gender> interestedIn,
        @Min(120) @Max(230) Integer heightCm,

        @NotEmpty @Size(max = 3) Set<RelationshipIntent> lookingFor,
        @Size(max = 10) Set<Interest> interests,
        @Size(max = 5) List<@NotBlank @Size(max = 100) String> hangoutPlaces,
        @Size(max = 3) List<@Valid PromptDto> prompts,

        Habit drinking,
        Habit smoking,
        Habit workout,

        @Size(max = 100) String jobTitle,
        @Size(max = 100) String company,
        @Size(max = 150) String education,
        @Size(max = 5) Set<@NotBlank @Size(max = 40) String> languages,

        @NotBlank @Size(max = 80) String city,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,

        @Size(max = 6) List<@NotBlank @Size(max = 500) String> photoUrls,

        @Min(18) @Max(100) Integer ageMin,
        @Min(18) @Max(100) Integer ageMax,
        @Min(1) @Max(500) Integer maxDistanceKm
) {
    @AssertTrue(message = "ageMin must be less than or equal to ageMax")
    public boolean isAgeRangeValid() {
        return ageMin == null || ageMax == null || ageMin <= ageMax;
    }
}