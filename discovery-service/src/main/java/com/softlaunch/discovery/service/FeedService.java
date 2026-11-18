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
    private static final int VERIFIED_POINTS = 10;      // trust boost, small enough not to beat a great match
    // "Similar vibe" from profile embeddings (ai-service). MEASURED with nomic-embed-text on sample profiles:
    // unrelated people score ~0.73-0.80 (all profile texts share the same layout), two trekkers with no
    // words in common ~0.90. So points scale from VIBE_FLOOR (0) to VIBE_TOP (max). Re-measure if the model changes.
    private static final int MAX_VIBE_POINTS = 15;
    private static final double VIBE_FLOOR = 0.78;
    private static final double VIBE_TOP = 0.92;
    private static final double SIMILAR_VIBE_LABEL = 0.90;   // the label must feel earned: only clearly alike profiles

    private final LocationService locationService;
    private final UserProfileClient userProfileClient;
    private final FeedCache feedCache;
    private final SafetyFilter safetyFilter;
    private final VibeScores vibeScores;
    private final PrivateModeFilter privateMode;

    public FeedService(LocationService locationService, UserProfileClient userProfileClient, FeedCache feedCache,
                       SafetyFilter safetyFilter, VibeScores vibeScores, PrivateModeFilter privateMode) {
        this.privateMode = privateMode;
        this.vibeScores = vibeScores;
        this.locationService = locationService;
        this.userProfileClient = userProfileClient;
        this.feedCache = feedCache;
        this.safetyFilter = safetyFilter;
    }

    public List<FeedCard> feed(UUID me, int radiusKm) {
        List<FeedCard> feed = feedCache.get(me, radiusKm).orElseGet(() -> {
            List<FeedCard> fresh = buildFeed(me, radiusKm);
            feedCache.put(me, radiusKm, fresh);
            return fresh;
        });
        // Applied on EVERY read, even cached feeds: a block, a review or Private Mode must take effect immediately.
        Set<String> hidden = safetyFilter.hiddenFrom(me);
        List<FeedCard> visible = hidden.isEmpty() ? feed
                : feed.stream().filter(card -> !hidden.contains(card.userId().toString())).toList();
        return privateMode.apply(me, visible);
    }

    private List<FeedCard> buildFeed(UUID me, int radiusKm) {
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

        List<NearbyUserResponse> candidates = nearby.stream()
                .filter(n -> profiles.containsKey(n.userId()))
                .filter(n -> isMutualFit(myProfile, profiles.get(n.userId())))
                .toList();
        // location + preferences decide WHO is in the feed; the AI only helps ORDER them
        Map<UUID, Double> vibe = vibeScores.forCandidates(me, candidates.stream().map(NearbyUserResponse::userId).toList());

        return candidates.stream()
                .map(n -> toCard(myProfile, profiles.get(n.userId()), n.distanceKm(), radiusKm, vibe.get(n.userId())))
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

    private FeedCard toCard(FeedProfile me, FeedProfile them, int distanceKm, int radiusKm, Double vibeSimilarity) {
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

        int verifiedPoints = them.verified() ? VERIFIED_POINTS : 0;
        if (them.verified()) {
            reasons.add("Verified ✅");
        }

        int vibePoints = vibePoints(vibeSimilarity);
        if (vibeSimilarity != null && vibeSimilarity >= SIMILAR_VIBE_LABEL) {
            reasons.add("Similar vibe ✨");
        }

        int score = distancePoints + interestPoints + intentPoints + completenessPoints + verifiedPoints + vibePoints;

        return new FeedCard(them.userId(), them.displayName(), them.verified(), them.age(), distanceKm, them.bio(), them.city(),
                them.lookingFor(), shared, them.photoUrls(), reasons, score, Boolean.TRUE.equals(them.veiledPhotos()));
    }
    static int vibePoints(Double similarity) {
        if (similarity == null) return 0;   // no embedding yet, or ai-service unavailable
        double scaled = (similarity - VIBE_FLOOR) / (VIBE_TOP - VIBE_FLOOR);
        return (int) Math.round(MAX_VIBE_POINTS * Math.max(0, Math.min(1, scaled)));
    }

    private boolean isMutualFit(FeedProfile me, FeedProfile them) {
        return wants(me, them) && wants(them, me);
    }

    private boolean wants(FeedProfile seeker, FeedProfile candidate) {
        boolean genderOk = seeker.interestedIn() == null || seeker.interestedIn().isEmpty()
                || (candidate.gender() != null && seeker.interestedIn().contains(candidate.gender()));

        boolean ageOk = candidate.age() == null
                || ((seeker.ageMin() == null || candidate.age() >= seeker.ageMin())
                && (seeker.ageMax() == null || candidate.age() <= seeker.ageMax()));

        return genderOk && ageOk;
    }
}