package com.softlaunch.chat.sticker;

import java.util.Set;

/** The ids the app may send. Drawings live in the app; the server only checks the id. Flirty, never explicit. */
public final class Stickers {
    private Stickers() {
    }

    /** Free for everyone. */
    public static final Set<String> FREE = Set.of("lips", "uup", "fire", "comehere", "wink", "tonight",
            "peach", "missyou", "moon", "badidea", "hearts", "yourplace");

    /** Animated gifts: 3 ❤ each. */
    public static final Set<String> GIFTS = Set.of("rose", "champagne", "chocolates", "lovenote", "fireworks", "crown");
}
