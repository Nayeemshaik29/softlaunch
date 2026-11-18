package com.softlaunch.user.photo;

import java.util.List;

/** Turns stored photo keys into links a phone can open. Implemented by PhotoService (signed, expiring links). */
@FunctionalInterface
public interface PhotoLinks {
    List<String> of(List<String> photoKeys);
}
