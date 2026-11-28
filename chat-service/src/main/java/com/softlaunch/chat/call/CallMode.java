package com.softlaunch.chat.call;

/** What a person agreed to for one match. A call may only use what BOTH agreed to (the lower of the two). */
public enum CallMode {
    OFF, AUDIO, VIDEO;

    public static CallMode lowerOf(CallMode a, CallMode b) {
        return a.ordinal() <= b.ordinal() ? a : b;
    }
}
