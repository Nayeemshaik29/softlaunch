package com.softlaunch.ai.openers;

import com.softlaunch.ai.client.UserProfileClient.ProfileView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Template openers: used when the AI is slow, down, or its answers fail OpenerSafety. Always works, no LLM. */
final class FallbackOpeners {

    private FallbackOpeners() {
    }

    static List<String> forProfiles(ProfileView me, ProfileView them) {
        String name = OpenerPrompt.firstName(them.displayName());
        Set<String> shared = OpenerPrompt.shared(me, them);
        List<String> result = new ArrayList<>();
        shared.stream().limit(2).map(Interests::readable).forEach(interest ->
                result.add("Hey " + name + "! We both like " + interest + ". What got you into it?"));
        if (them.interests() != null) {
            them.interests().stream().filter(i -> !shared.contains(i)).sorted().findFirst().map(Interests::readable)
                    .ifPresent(interest -> result.add("Hi " + name + ", I saw you're into " + interest
                            + ". What's the best part about it?"));
        }
        if (them.city() != null && !them.city().isBlank()) {
            result.add("Hey " + name + "! What's your favourite weekend spot in " + them.city().strip() + "?");
        }
        result.add("Hi " + name + "! What's something you're looking forward to this week?");
        result.add("Hey " + name + ", tea or coffee? And where's the best one near you?");
        return result.stream().distinct().limit(3).toList();
    }
}
