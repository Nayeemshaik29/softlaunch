package com.softlaunch.user.dto;

import com.softlaunch.user.model.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record MyProfileResponse(
        UUID userId,
        String displayName,
        Integer age,
        String bio,
        String pronouns,
        Gender gender,
        Set<Gender> interestedIn,
        Integer heightCm,
        Set<RelationshipIntent> lookingFor,
        Set<Interest> interests,
        List<String> hangoutPlaces,
        List<PromptDto> prompts,
        Habit drinking,
        Habit smoking,
        Habit workout,
        String jobTitle,
        String company,
        String education,
        Set<String> languages,
        String city,
        Double latitude,
        Double longitude,
        List<String> photoUrls,
        Integer ageMin,
        Integer ageMax,
        Integer maxDistanceKm,
        Instant updatedAt
) {
    public static MyProfileResponse from(Profile p) {
        User user = p.getUser();
        return new MyProfileResponse(
                p.getUserId(),
                user.getDisplayName(),
                ageOf(user.getDateOfBirth()),
                p.getBio(),
                p.getPronouns(),
                p.getGender(),
                Set.copyOf(p.getInterestedIn()),
                p.getHeightCm(),
                Set.copyOf(p.getLookingFor()),
                Set.copyOf(p.getInterests()),
                List.copyOf(p.getHangoutPlaces()),
                p.getPrompts().stream().map(PromptDto::from).toList(),
                p.getDrinking(),
                p.getSmoking(),
                p.getWorkout(),
                p.getJobTitle(),
                p.getCompany(),
                p.getEducation(),
                Set.copyOf(p.getLanguages()),
                p.getCity(),
                p.getLatitude(),
                p.getLongitude(),
                List.copyOf(p.getPhotoUrls()),
                p.getAgeMin(),
                p.getAgeMax(),
                p.getMaxDistanceKm(),
                p.getUpdatedAt()
        );
    }

    static Integer ageOf(LocalDate dateOfBirth) {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}