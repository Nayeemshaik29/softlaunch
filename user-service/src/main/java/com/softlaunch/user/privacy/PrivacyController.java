package com.softlaunch.user.privacy;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/privacy")
public class PrivacyController {

    private final PrivacyService privacy;

    public PrivacyController(PrivacyService privacy) {
        this.privacy = privacy;
    }

    @GetMapping
    public PrivacySettings get(@RequestHeader("X-User-Id") UUID me) {
        return privacy.get(me);
    }

    @PutMapping
    public PrivacySettings update(@RequestHeader("X-User-Id") UUID me, @RequestBody PrivacySettings change) {
        return privacy.update(me, change);
    }
}
