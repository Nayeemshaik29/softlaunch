package com.softlaunch.user.service;

import com.softlaunch.user.dto.MyProfileResponse;
import com.softlaunch.user.dto.ProfileRequest;
import com.softlaunch.user.dto.PromptDto;
import com.softlaunch.user.exception.ProfileNotFoundException;
import com.softlaunch.user.exception.UserNotFoundException;
import com.softlaunch.user.model.Profile;
import com.softlaunch.user.model.User;
import com.softlaunch.user.repository.ProfileRepository;
import com.softlaunch.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileService(ProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(UUID userId) {
        return profileRepository.findById(userId)
                .map(MyProfileResponse::from)
                .orElseThrow(() -> new ProfileNotFoundException(userId));
    }

    @Transactional
    public MyProfileResponse upsertMyProfile(UUID userId, ProfileRequest request) {
        Profile profile = profileRepository.findById(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new UserNotFoundException(userId));
                    return new Profile(user);
                });

        apply(profile, request);
        profile.touch();

        return MyProfileResponse.from(profileRepository.save(profile));
    }

    private void apply(Profile p, ProfileRequest r) {
        p.setBio(r.bio());
        p.setPronouns(r.pronouns());
        p.setGender(r.gender());
        p.setHeightCm(r.heightCm());
        p.setDrinking(r.drinking());
        p.setSmoking(r.smoking());
        p.setWorkout(r.workout());
        p.setJobTitle(r.jobTitle());
        p.setCompany(r.company());
        p.setEducation(r.education());
        p.setCity(r.city());
        p.setLatitude(r.latitude());
        p.setLongitude(r.longitude());
        p.setAgeMin(r.ageMin());
        p.setAgeMax(r.ageMax());
        p.setMaxDistanceKm(r.maxDistanceKm());

        replace(p.getInterestedIn(), r.interestedIn());
        replace(p.getLookingFor(), r.lookingFor());
        replace(p.getInterests(), r.interests());
        replace(p.getHangoutPlaces(), r.hangoutPlaces());
        replace(p.getLanguages(), r.languages());
        replace(p.getPhotoUrls(), r.photoUrls());

        p.getPrompts().clear();
        if (r.prompts() != null) {
            r.prompts().stream().map(PromptDto::toEntity).forEach(p.getPrompts()::add);
        }
    }

    private static <T> void replace(Collection<T> target, Collection<T> source) {
        target.clear();
        if (source != null) {
            target.addAll(source);
        }
    }
}