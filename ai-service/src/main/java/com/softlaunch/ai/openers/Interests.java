package com.softlaunch.ai.openers;

import java.util.Locale;
import java.util.Map;

final class Interests {

    // names that read badly after a plain lower-case ("We both like foodie")
    private static final Map<String, String> NATURAL = Map.of(
            "FOODIE", "good food",
            "STANDUP_COMEDY", "stand-up comedy",
            "GYM", "the gym",
            "TECH", "tech");

    private Interests() {
    }

    /** FOODIE -> "good food", BOARD_GAMES -> "board games" (used in templates AND in the LLM prompt) */
    static String readable(String enumName) {
        return NATURAL.getOrDefault(enumName, enumName.toLowerCase(Locale.ROOT).replace('_', ' '));
    }
}
