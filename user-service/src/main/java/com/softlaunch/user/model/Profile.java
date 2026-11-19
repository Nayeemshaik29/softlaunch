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
    // Storage KEYS of processed photos (e.g. photos/<user>/<id>.jpg), in display order. Never URLs typed by
    // the user: links are created on the way out (signed, expiring). Uploads go through PhotoService only.
    @ElementCollection
    @CollectionTable(name = "profile_photo_keys", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "position")
    @Column(name = "object_key", length = 200)
    private List<String> photoKeys = new ArrayList<>();

    // ---- Discovery preferences (PRIVATE) ----
    private Integer ageMin;
    private Integer ageMax;
    private Integer maxDistanceKm;

    @Column(nullable = false)
    private Instant updatedAt;

    /** Private Mode: only people this user has liked can see them in Discover; online / last seen hidden. */
    @org.hibernate.annotations.ColumnDefault("false")
    @Column(nullable = false)
    private boolean privateMode;

    /** Desires (After dark): one row per desire with its level. NOT_INTO never leaves user-service/discovery. */
    @ElementCollection
    @CollectionTable(name = "profile_desires", joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "desire", length = 40)
    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 20)
    private java.util.Map<Desire, DesireLevel> desires = new java.util.EnumMap<>(Desire.class);

    /** Veiled photos: others see a blurred copy until there's a match. */
    @org.hibernate.annotations.ColumnDefault("false")
    @Column(nullable = false)
    private boolean veiledPhotos;

    protected Profile() {
    }

    public Profile(User user) {
        this.user = user;
        this.updatedAt = Instant.now();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPronouns() {
        return pronouns;
    }

    public void setPronouns(String pronouns) {
        this.pronouns = pronouns;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Set<Gender> getInterestedIn() {
        return interestedIn;
    }

    public void setInterestedIn(Set<Gender> interestedIn) {
        this.interestedIn = interestedIn;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Integer heightCm) {
        this.heightCm = heightCm;
    }

    public Set<RelationshipIntent> getLookingFor() {
        return lookingFor;
    }

    public void setLookingFor(Set<RelationshipIntent> lookingFor) {
        this.lookingFor = lookingFor;
    }

    public Set<Interest> getInterests() {
        return interests;
    }

    public void setInterests(Set<Interest> interests) {
        this.interests = interests;
    }

    public List<String> getHangoutPlaces() {
        return hangoutPlaces;
    }

    public void setHangoutPlaces(List<String> hangoutPlaces) {
        this.hangoutPlaces = hangoutPlaces;
    }

    public List<Prompt> getPrompts() {
        return prompts;
    }

    public void setPrompts(List<Prompt> prompts) {
        this.prompts = prompts;
    }

    public Habit getDrinking() {
        return drinking;
    }

    public void setDrinking(Habit drinking) {
        this.drinking = drinking;
    }

    public Habit getSmoking() {
        return smoking;
    }

    public void setSmoking(Habit smoking) {
        this.smoking = smoking;
    }

    public Habit getWorkout() {
        return workout;
    }

    public void setWorkout(Habit workout) {
        this.workout = workout;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public Set<String> getLanguages() {
        return languages;
    }

    public void setLanguages(Set<String> languages) {
        this.languages = languages;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public List<String> getPhotoKeys() {
        return photoKeys;
    }


    public Integer getAgeMin() {
        return ageMin;
    }

    public void setAgeMin(Integer ageMin) {
        this.ageMin = ageMin;
    }

    public Integer getAgeMax() {
        return ageMax;
    }

    public void setAgeMax(Integer ageMax) {
        this.ageMax = ageMax;
    }

    public Integer getMaxDistanceKm() {
        return maxDistanceKm;
    }

    public void setMaxDistanceKm(Integer maxDistanceKm) {
        this.maxDistanceKm = maxDistanceKm;
    }

    public java.util.Map<Desire, DesireLevel> getDesires() { return desires; }

    public java.util.Set<String> desiresAt(DesireLevel level) {
        return desires.entrySet().stream().filter(e -> e.getValue() == level).map(e -> e.getKey().name())
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
    }

    public boolean isPrivateMode() { return privateMode; }
    public void setPrivateMode(boolean privateMode) { this.privateMode = privateMode; }
    public boolean isVeiledPhotos() { return veiledPhotos; }
    public void setVeiledPhotos(boolean veiledPhotos) { this.veiledPhotos = veiledPhotos; }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}