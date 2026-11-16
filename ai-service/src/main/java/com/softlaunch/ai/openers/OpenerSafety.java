package com.softlaunch.ai.openers;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Last line of defence: every AI opener is checked before a user sees it.
 * LLMs ignore rules sometimes (or get tricked by a bio), so the code enforces the important ones itself.
 */
final class OpenerSafety {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 160;
    private static final Pattern LINK = Pattern.compile("(https?://|www\\.|\\.(com|in|net|org|io|me)\\b)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_OR_ID = Pattern.compile("\\d{6,}|@");
    private static final List<String> BLOCKED = List.of(
            // moving off-platform / money: classic scam openers
            "whatsapp", "telegram", "instagram", "snapchat", "insta id", "phone number", "your number",
            "upi", "paytm", "bitcoin", "crypto", "invest",
            // sexual / looks
            "sexy", "hot ", "body", "nude", "bed",
            // signs the model followed injected instructions or leaked the prompt
            "ignore", "instruction", "system prompt", "as an ai");

    private OpenerSafety() {
    }

    static boolean isAcceptable(String opener) {
        if (opener == null) return false;
        String text = opener.strip();
        if (text.length() < MIN_LENGTH || text.length() > MAX_LENGTH) return false;
        if (LINK.matcher(text).find() || PHONE_OR_ID.matcher(text).find()) return false;
        String lower = text.toLowerCase(Locale.ROOT) + " ";
        return BLOCKED.stream().noneMatch(lower::contains);
    }
}
