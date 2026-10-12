package com.softlaunch.user.dto;

import com.softlaunch.user.model.Profile;
import com.softlaunch.user.model.User;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record InternalProfileResponse(
        UUID userId,
        String displayName,
        Integer age,
        String bio,
        String city,
        String gender,
        Set<String> interestedIn,
        Integer ageMin,
        Integer ageMax,
        Set<String> lookingFor,
        Set<String> interests,
        List<String> photoUrls) {

    public static InternalProfileResponse from(Profile p) {
        User user = p.getUser();
        return new InternalProfileResponse(
                p.getUserId(),
                user.getDisplayName(),
                MyProfileResponse.ageOf(user.getDateOfBirth()),
                p.getBio(),
                p.getCity(),
                p.getGender() == null ? null : p.getGender().name(),
                p.getInterestedIn().stream().map(Enum::name).collect(Collectors.toSet()),
                p.getAgeMin(),
                p.getAgeMax(),
                p.getLookingFor().stream().map(Enum::name).collect(Collectors.toSet()),
                p.getInterests().stream().map(Enum::name).collect(Collectors.toSet()),
                List.copyOf(p.getPhotoUrls()));
    }
}