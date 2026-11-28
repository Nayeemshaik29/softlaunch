package com.softlaunch.user.hearts;

/** What Hearts buy, and what each costs. Prices live here (server side) so the app can't change them. */
public enum HeartItem {
    CROWN(15),       // King of the Hill: top of nearby decks for 30 minutes
    NOTE(5),         // note with a like (match-service)
    GIFT(3),         // animated gift in chat (chat-service)
    KEEP_CHAT(5),    // chat lives 24 hours longer
    CALL_PASS(10);   // calls with one match until midnight (≈ ₹50)

    public final int cost;

    HeartItem(int cost) {
        this.cost = cost;
    }
}
