package com.softlaunch.user.model;


import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // ---- About me ----
    @Column(length = 500)
    private String bio;

    @Column(length = 30)
    private String pronouns;

    // ---- Identity ----
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @ElementCollection
    @CollectionTable(name = "profile_interested_in", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Set<Gender> interestedIn = new HashSet<>();

    private Integer heightCm;

    // ---- Looking for ----
    @ElementCollection
    @CollectionTable(name = "profile_looking_for", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "intent", length = 40)
    private Set<RelationshipIntent> lookingFor = new HashSet<>();

    // ---- Interests & places ----
    @ElementCollection
    @CollectionTable(name = "profile_interests", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "interest", length = 30)
    private Set<Interest> interests = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "profile_hangout_places", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "place", length = 100)
    private List<String> hangoutPlaces = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "profile_prompts", joinColumns = @JoinColumn(name = "user_id"))
    private List<Prompt> prompts = new ArrayList<>();

    // ---- Lifestyle ----
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Habit drinking;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Habit smoking;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Habit workout;

    // ---- Work & education ----
    @Column(length = 100)
    private String jobTitle;

    @Column(length = 100)
    private String company;

    @Column(length = 150)
    private String education;

    @ElementCollection
    @CollectionTable(name = "profile_languages", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "language", length = 40)
    private Set<String> languages = new HashSet<>();

    // ---- Location (lat/lng are PRIVATE) ----
    @Column(length = 80)
    private String city;

    private Double latitude;
    private Double longitude;

    // ---- Photos ----
    @ElementCollection
    @CollectionTable(name = "profile_photos", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "url", length = 500)
    private List<String> photoUrls = new ArrayList<>();

    // ---- Discovery preferences (PRIVATE) ----
    private Integer ageMin;
    private Integer ageMax;
    private Integer maxDistanceKm;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Profile() {
    }

    public Profile(User user) {
        this.user = user;
        this.updatedAt = Instant.now();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}