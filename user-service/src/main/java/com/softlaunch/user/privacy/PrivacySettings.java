package com.softlaunch.user.privacy;

/** PUT body and response. null in a request = "leave as it is". */
public record PrivacySettings(Boolean privateMode, Boolean veiledPhotos) {
}
