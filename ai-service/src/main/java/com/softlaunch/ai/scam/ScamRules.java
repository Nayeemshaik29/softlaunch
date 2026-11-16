package com.softlaunch.ai.scam;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Filter 1: cheap regex rules (microseconds). Every message goes through these; only messages that
 * score >= SUSPICIOUS go on to the (slow) LLM. Each signal has a weight; one weak signal alone
 * ("let's talk on WhatsApp") is normal dating-app talk, so it does NOT reach the LLM.
 */
public final class ScamRules {

    public static final int SUSPICIOUS = 3;   // worth asking the LLM
    public static final int STRONG = 6;       // report even when the LLM is down

    public enum Signal {
        UPI_ID(4, "[a-z0-9._-]{2,}@(ybl|okaxis|oksbi|okhdfcbank|okicici|paytm|upi|ibl|axl|apl|ptyes|ptsbi|yespay|freecharge)\\b"),
        MONEY_REQUEST(3, "\\b(send|transfer|pay|lend|need)\\b.{0,25}(money|rs\\.?|inr|₹|\\d{3,}|amount|cash)|gift ?card|\\bloan\\b|gpay|g pay|phonepe|paytm|google pay"),
        CRYPTO_INVESTMENT(3, "bitcoin|\\bbtc\\b|usdt|crypto|forex|trading (app|platform|account)|\\binvest|guaranteed (return|profit)|double (your|the) money|\\d+x (return|profit)|profit daily"),
        LINK(2, "https?://|www\\.|bit\\.ly|t\\.me/|\\b[a-z0-9-]+\\.(com|in|xyz|top|io|net|app|link)\\b"),
        OFF_PLATFORM(2, "whatsapp|telegram|\\bsignal app|snapchat|\\bsnap me|call me on|my number"),
        PHONE_NUMBER(2, "(\\+91[\\s-]?)?\\b[6-9]\\d{9}\\b"),
        URGENCY(1, "urgent|emergency|hospital|accident|stuck at|customs|help me|asap|immediately|right now"),
        // someone talking to the AI/moderator instead of their date: a red flag on its own,
        // and the reason we don't trust an LLM "not a scam" when it appears (prompt injection)
        MANIPULATION(3, "ignore (all |your |the |any |previous )*(rules|instructions|prompt)|not a scam|scam\\W{0,3}(=|:|is)?\\W{0,3}false|note (for|to) the (reviewer|moderator|ai|system)|system prompt|you are an ai");

        final int weight;
        final Pattern pattern;

        Signal(int weight, String regex) {
            this.weight = weight;
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
    }

    public record Result(int score, Set<Signal> signals) {
        public boolean suspicious() { return score >= SUSPICIOUS; }
        public boolean strong() { return score >= STRONG; }
    }

    private ScamRules() {
    }

    public static Result check(String text) {
        if (text == null || text.isBlank()) {
            return new Result(0, Set.of());
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        Set<Signal> found = EnumSet.noneOf(Signal.class);
        int score = 0;
        for (Signal signal : Signal.values()) {
            if (signal.pattern.matcher(normalized).find()) {
                found.add(signal);
                score += signal.weight;
            }
        }
        return new Result(score, found);
    }
}
