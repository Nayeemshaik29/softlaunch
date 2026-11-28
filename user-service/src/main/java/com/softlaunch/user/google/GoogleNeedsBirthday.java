package com.softlaunch.user.google;

/** A new person signed in with Google: ask their birthday (18+) before we create the account. */
public record GoogleNeedsBirthday(String status, String email, String suggestedName) {
    public GoogleNeedsBirthday(String email, String suggestedName) {
        this("NEEDS_BIRTHDAY", email, suggestedName);
    }
}
