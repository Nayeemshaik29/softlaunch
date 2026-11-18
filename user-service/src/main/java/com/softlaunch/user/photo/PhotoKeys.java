package com.softlaunch.user.photo;

import java.util.UUID;

final class PhotoKeys {

    private PhotoKeys() {
    }

    static String of(UUID userId, UUID photoId) {
        return "photos/" + userId + "/" + photoId + ".jpg";
    }
}
