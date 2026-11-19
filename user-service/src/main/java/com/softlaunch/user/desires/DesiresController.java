package com.softlaunch.user.desires;

import com.softlaunch.user.event.ProfileUpdatedEvent;
import com.softlaunch.user.exception.ProfileNotFoundException;
import com.softlaunch.user.model.Desire;
import com.softlaunch.user.model.DesireLevel;
import com.softlaunch.user.model.Profile;
import com.softlaunch.user.repository.ProfileRepository;
import jakarta.validation.Valid;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Desires: optional, opt-in, private by default. Others only ever see the ones you SHARE with them. */
@RestController
@RequestMapping("/api/users/me/profile/desires")
public class DesiresController {

    private final ProfileRepository profiles;
    private final ApplicationEventPublisher events;

    public DesiresController(ProfileRepository profiles, ApplicationEventPublisher events) {
        this.profiles = profiles;
        this.events = events;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public DesiresDto get(@RequestHeader("X-User-Id") UUID me) {
        return toDto(load(me).getDesires());
    }

    @PutMapping
    @Transactional
    public DesiresDto put(@RequestHeader("X-User-Id") UUID me, @Valid @RequestBody DesiresDto body) {
        Profile p = load(me);
        Map<Desire, DesireLevel> d = p.getDesires();
        d.clear();
        body.into().forEach(x -> d.put(x, DesireLevel.INTO));
        body.openTo().forEach(x -> d.put(x, DesireLevel.OPEN_TO));
        body.notInto().forEach(x -> d.put(x, DesireLevel.NOT_INTO));
        p.touch();
        events.publishEvent(ProfileUpdatedEvent.of(me));   // feeds recompute mutual desires
        return toDto(d);
    }

    private Profile load(UUID me) {
        return profiles.findById(me).orElseThrow(() -> new ProfileNotFoundException(me));
    }

    private static DesiresDto toDto(Map<Desire, DesireLevel> d) {
        return new DesiresDto(at(d, DesireLevel.INTO), at(d, DesireLevel.OPEN_TO), at(d, DesireLevel.NOT_INTO));
    }

    private static Set<Desire> at(Map<Desire, DesireLevel> d, DesireLevel level) {
        return d.entrySet().stream().filter(e -> e.getValue() == level).map(Map.Entry::getKey)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Desire.class)));
    }
}
