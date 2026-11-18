package com.softlaunch.user.photo;

import java.util.UUID;

final class PhotoKeys {

    private PhotoKeys() {
    }

    static String of(UUID userId, UUID photoId) {
        return "photos/" + userId + "/" + photoId + ".jpg";
    }

    /** The blurred copy shown before a match: photos/<user>/<photoId>.veil.jpg */
    static String veilOf(String key) {
        return key.substring(0, key.length() - ".jpg".length()) + ".veil.jpg";
    }

    /** "photos/<user>/<photoId>.jpg" → photoId */
    static UUID idOf(String key) {
        String file = key.substring(key.lastIndexOf('/') + 1);
        return UUID.fromString(file.substring(0, file.length() - ".jpg".length()));
    }
}
