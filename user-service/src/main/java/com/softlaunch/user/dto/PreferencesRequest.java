package com.softlaunch.user.dto;

import com.softlaunch.user.model.Gender;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/** Discover filters: who I want to see. Only these four fields change; the rest of the profile stays as it is. */
public record PreferencesRequest(
        @NotNull @Min(18) @Max(100) Integer ageMin,
        @NotNull @Min(18) @Max(100) Integer ageMax,
        @NotNull @Min(1) @Max(100) Integer maxDistanceKm,   // the feed searches up to 100 km
        @NotEmpty Set<Gender> interestedIn) {

    @AssertTrue(message = "ageMin must be less than or equal to ageMax")
    public boolean isAgeRangeValid() {
        return ageMin == null || ageMax == null || ageMin <= ageMax;
    }
}
