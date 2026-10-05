package com.softlaunch.user.dto;

import com.softlaunch.user.model.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PublicProfileResponse(
        UUID userId,
        String displayName,
        Integer age,
        String bio,
        String pronouns,
        Gender gender,
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
        List<String> photoUrls
) {
    public static PublicProfileResponse from(Profile p) {
        User user = p.getUser();
        return new PublicProfileResponse(
                p.getUserId(),
                user.getDisplayName(),
                MyProfileResponse.ageOf(user.getDateOfBirth()),
                p.getBio(),
                p.getPronouns(),
                p.getGender(),
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
                List.copyOf(p.getPhotoUrls())
        );
    }
}