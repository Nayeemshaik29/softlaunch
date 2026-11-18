package com.softlaunch.user.service;

import com.softlaunch.user.dto.PublicProfileResponse;
import com.softlaunch.user.model.Profile;
import com.softlaunch.user.model.User;
import com.softlaunch.user.photo.PhotoLinks;
import com.softlaunch.user.privacy.MatchChecker;
import com.softlaunch.user.repository.ProfileRepository;
import com.softlaunch.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** The rule: a veiled profile's REAL photo links only go to the owner and to matches. */
class VeiledProfileTest {

    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final MatchChecker matches = mock(MatchChecker.class);
    private final PhotoLinks links = keys -> keys.stream().map(k -> "https://s3/" + k).toList();
    private ProfileService service;
    private final UUID owner = UUID.randomUUID();
    private final UUID stranger = UUID.randomUUID();
    private Profile profile;

    @BeforeEach
    void setUp() {
        service = new ProfileService(profiles, mock(UserRepository.class), mock(ApplicationEventPublisher.class), links, matches);
        User user = new User("asha@test.com", "hash", "Asha", LocalDate.of(1998, 5, 10));
        profile = mock(Profile.class, RETURNS_DEFAULTS);
        when(profile.getUser()).thenReturn(user);
        when(profile.getUserId()).thenReturn(owner);
        when(profile.getPhotoKeys()).thenReturn(List.of("photos/" + owner + "/p1.jpg"));
        when(profile.isVeiledPhotos()).thenReturn(true);
        when(profiles.findById(owner)).thenReturn(Optional.of(profile));
    }

    @Test
    void strangersGetOnlyTheBlurredCopy() {
        PublicProfileResponse r = service.getPublicProfile(owner, stranger);
        assertThat(r.veiled()).isTrue();
        assertThat(r.photoUrls()).allMatch(u -> u.endsWith(".veil.jpg"));
    }

    @Test
    void matchesSeeTheRealPhotos() {
        when(matches.matched(stranger, owner)).thenReturn(true);
        PublicProfileResponse r = service.getPublicProfile(owner, stranger);
        assertThat(r.veiled()).isFalse();
        assertThat(r.photoUrls()).noneMatch(u -> u.contains(".veil."));
    }

    @Test
    void theOwnerSeesTheirOwnPhotosWithoutAskingMatchService() {
        assertThat(service.getPublicProfile(owner, owner).veiled()).isFalse();
        verifyNoInteractions(matches);
    }

    @Test
    void unveiledProfilesNeverCallMatchService() {
        when(profile.isVeiledPhotos()).thenReturn(false);
        assertThat(service.getPublicProfile(owner, stranger).veiled()).isFalse();
        verify(matches, never()).matched(any(), any());
    }
}
