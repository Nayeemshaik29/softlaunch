package com.softlaunch.user.service;

import com.softlaunch.user.model.Profile;

public final class ProfileCompletion {

    private ProfileCompletion() {
    }

    public static int percent(Profile p) {
        boolean[] checks = {
                hasText(p.getBio()),
                p.getGender() != null,
                !p.getInterestedIn().isEmpty(),
                !p.getLookingFor().isEmpty(),
                p.getInterests().size() >= 3,
                !p.getHangoutPlaces().isEmpty(),
                !p.getPrompts().isEmpty(),
                !p.getPhotoUrls().isEmpty(),
                hasText(p.getJobTitle()) || hasText(p.getEducation()),
                hasText(p.getCity()),
                p.getDrinking() != null || p.getSmoking() != null || p.getWorkout() != null
        };

        int done = 0;
        for (boolean check : checks) {
            if (check) {
                done++;
            }
        }
        return done * 100 / checks.length;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}