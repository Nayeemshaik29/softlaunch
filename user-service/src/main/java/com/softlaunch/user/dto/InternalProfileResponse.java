package com.softlaunch.user.dto;

import com.softlaunch.user.model.Profile;
import com.softlaunch.user.photo.PhotoLinks;
import com.softlaunch.user.model.User;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record InternalProfileResponse(
        UUID userId,
        String displayName,
        boolean verified,
        Integer age,
        String bio,
        String city,
        String gender,
        Set<String> interestedIn,
        Integer ageMin,
        Integer ageMax,
        Set<String> lookingFor,
        Set<String> interests,
        List<String> photoUrls,
        List<String> hangoutPlaces,
        List<PromptAnswer> prompts) {

    /** Free-text answers ("My ideal Sunday: ..."): the richest signal for the AI's "similar vibe" matching. */
    public record PromptAnswer(String question, String answer) {
    }

    public static InternalProfileResponse from(Profile p, PhotoLinks photoLinks) {
        User user = p.getUser();
        return new InternalProfileResponse(
                p.getUserId(),
                user.getDisplayName(),
                user.isVerified(),
                MyProfileResponse.ageOf(user.getDateOfBirth()),
                p.getBio(),
                p.getCity(),
                p.getGender() == null ? null : p.getGender().name(),
                p.getInterestedIn().stream().map(Enum::name).collect(Collectors.toSet()),
                p.getAgeMin(),
                p.getAgeMax(),
                p.getLookingFor().stream().map(Enum::name).collect(Collectors.toSet()),
                p.getInterests().stream().map(Enum::name).collect(Collectors.toSet()),
                photoLinks.of(p.getPhotoKeys()),
                List.copyOf(p.getHangoutPlaces()),
                p.getPrompts().stream().map(q -> new PromptAnswer(q.getQuestion(), q.getAnswer())).toList());
    }
}