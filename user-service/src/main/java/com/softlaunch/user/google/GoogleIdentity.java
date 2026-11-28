package com.softlaunch.user.google;

/** What we take from a verified Google ID token. Never logged: email and name are personal data. */
public record GoogleIdentity(String sub, String email, String name) {
}
