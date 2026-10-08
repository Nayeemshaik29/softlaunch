package com.softlaunch.discovery.service;

import com.softlaunch.discovery.client.UserProfileClient;
import com.softlaunch.discovery.dto.FeedCard;
import com.softlaunch.discovery.dto.FeedProfile;
import com.softlaunch.discovery.dto.NearbyUserResponse;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FeedService {

    private static final int MAX_DISTANCE_POINTS = 40;
    private static final int POINTS_PER_SHARED_INTEREST = 8;
    private static final int MAX_SHARED_INTERESTS_COUNTED = 4;
    private static final int INTENT_MATCH_POINTS = 20;
    private static final int COMPLETE_PROFILE_POINTS = 4;

    private final LocationService locationService;
    private final UserProfileClient userProfileClient;

    public FeedService(LocationService locationService, UserProfileClient userProfileClient) {
        this.locationService = locationService;
        this.userProfileClient = userProfileClient;
    }

    public List<FeedCard> feed(UUID me, int radiusKm) {
        List<NearbyUserResponse> nearby = locationService.nearby(me, radiusKm);
        if (nearby.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = new ArrayList<>();
        ids.add(me);
        nearby.forEach(n -> ids.add(n.userId()));

        Map<UUID, FeedProfile> profiles = loadProfiles(ids);
        FeedProfile myProfile = profiles.get(me);
        if (myProfile == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Complete your profile first");
        }

        return nearby.stream()
                .filter(n -> profiles.containsKey(n.userId()))
                .map(n -> toCard(myProfile, profiles.get(n.userId()), n.distanceKm(), radiusKm))
                .sorted(Comparator.comparingInt(FeedCard::score).reversed())
                .toList();
    }

    private Map<UUID, FeedProfile> loadProfiles(List<UUID> ids) {
        try {
            return userProfileClient.getProfiles(ids).stream()
                    .collect(Collectors.toMap(FeedProfile::userId, Function.identity()));
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Feed temporarily unavailable");
        }
    }

    private FeedCard toCard(FeedProfile me, FeedProfile them, int distanceKm, int radiusKm) {
        List<String> reasons = new ArrayList<>();

        int distancePoints = (int) Math.round(
                MAX_DISTANCE_POINTS * (1 - Math.min(distanceKm, radiusKm) / (double) radiusKm));

        List<String> shared = them.interests().stream()
                .filter(me.interests()::contains)
                .sorted()
                .toList();
        int interestPoints = Math.min(shared.size(), MAX_SHARED_INTERESTS_COUNTED) * POINTS_PER_SHARED_INTEREST;
        if (!shared.isEmpty()) {
            reasons.add(shared.size() + " shared interest" + (shared.size() > 1 ? "s" : ""));
        }

        int intentPoints = 0;
        if (!Collections.disjoint(me.lookingFor(), them.lookingFor())) {
            intentPoints = INTENT_MATCH_POINTS;
            reasons.add("You want the same thing");
        }

        int completenessPoints = 0;
        if (them.bio() != null && !them.bio().isBlank()) completenessPoints += COMPLETE_PROFILE_POINTS;
        if (them.photoUrls() != null && !them.photoUrls().isEmpty()) completenessPoints += COMPLETE_PROFILE_POINTS;

        if (distanceKm <= 5) {
            reasons.add("Nearby");
        }

        int score = distancePoints + interestPoints + intentPoints + completenessPoints;

        return new FeedCard(them.userId(), them.displayName(), them.age(), distanceKm, them.bio(), them.city(),
                them.lookingFor(), shared, them.photoUrls(), reasons, score);
    }
}