package com.softlaunch.user.google;

/** How this account can sign in. Shown in Settings. */
public record SignInMethods(String email, boolean passwordSet, boolean googleLinked, boolean googleAvailable) {
}
