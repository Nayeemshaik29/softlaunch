package com.softlaunch.user.photo;

import java.util.UUID;

final class PhotoKeys {

    private PhotoKeys() {
    }

    static String of(UUID userId, UUID photoId) {
        return "photos/" + userId + "/" + photoId + ".jpg";
    }

    /** "photos/<user>/<photoId>.jpg" → photoId */
    static UUID idOf(String key) {
        String file = key.substring(key.lastIndexOf('/') + 1);
        return UUID.fromString(file.substring(0, file.length() - ".jpg".length()));
    }
}
