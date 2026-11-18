package com.softlaunch.user.photo;

import java.util.List;

/** Turns stored photo keys into links a phone can open. Implemented by PhotoService (signed, expiring links). */
@FunctionalInterface
public interface PhotoLinks {
    List<String> of(List<String> photoKeys);

    /** Same links, with each photo's id (the owner's app needs it to reorder or delete). */
    default List<PhotoRef> refs(List<String> photoKeys) {
        List<String> urls = of(photoKeys);
        return java.util.stream.IntStream.range(0, photoKeys.size())
                .mapToObj(i -> new PhotoRef(PhotoKeys.idOf(photoKeys.get(i)), urls.get(i))).toList();
    }

    record PhotoRef(java.util.UUID photoId, String url) {
    }
}
