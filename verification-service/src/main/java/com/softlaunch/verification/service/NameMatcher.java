package com.softlaunch.verification.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Loose check: is the first word of the display name one of the words in the legal name?
 * "Priya" vs "PRIYA SHARMA" = true. A soft signal only (people use nicknames), stored for moderators.
 */
public final class NameMatcher {

    private NameMatcher() { }

    public static boolean matches(String displayName, String legalName) {
        if (displayName == null || legalName == null) return false;
        List<String> shown = words(displayName);
        return !shown.isEmpty() && words(legalName).contains(shown.getFirst());
    }

    private static List<String> words(String s) {
        String plain = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")            // remove accents: "José" -> "Jose"
                .toLowerCase(Locale.ROOT);
        return Arrays.stream(plain.split("[^a-z]+")).filter(w -> !w.isBlank()).toList();
    }
}
