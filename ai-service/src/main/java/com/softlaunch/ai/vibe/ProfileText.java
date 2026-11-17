package com.softlaunch.ai.vibe;

import com.softlaunch.ai.client.UserProfileClient.ProfileView;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * The text whose MEANING we embed: what someone likes and how they describe themselves.
 * Deliberately left out: name, age, gender, city (location is handled by the geo filter) and
 * "looking for" (already scored separately). So the vibe score is only about personality and interests.
 */
final class ProfileText {

    // nomic-embed-text works best with a task prefix; "clustering:" fits "how alike are these two people"
    static final String TASK_PREFIX = "clustering: ";

    private ProfileText() {
    }

    /** Empty string when there's nothing meaningful to embed yet (new, empty profile). */
    static String of(ProfileView p) {
        StringJoiner text = new StringJoiner("\n");
        if (p.interests() != null && !p.interests().isEmpty()) {
            text.add("Interests: " + String.join(", ", p.interests().stream().sorted()
                    .map(i -> i.toLowerCase(Locale.ROOT).replace('_', ' ')).toList()) + ".");
        }
        if (p.bio() != null && !p.bio().isBlank()) {
            text.add("About me: " + p.bio().strip());
        }
        if (p.hangoutPlaces() != null && !p.hangoutPlaces().isEmpty()) {
            text.add("Favourite places: " + String.join(", ", p.hangoutPlaces()) + ".");
        }
        if (p.prompts() != null) {
            p.prompts().forEach(q -> text.add(q.question().strip() + " " + q.answer().strip()));
        }
        return text.toString();
    }

    static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
