package com.softlaunch.ai.openers;

import com.softlaunch.ai.client.UserProfileClient.ProfileView;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OpenerSafetyTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Hey! You like trekking too. Kedarkantha or Hampta Pass?",
            "Coffee first, adventures later: what's your favourite cafe in Bengaluru? ☕"
    })
    void acceptsNormalOpeners(String opener) {
        assertThat(OpenerSafety.isAcceptable(opener)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Add me on WhatsApp 9876543210!",                 // off-platform + phone number
            "Text me on telegram @rahul_deals",                // off-platform + handle
            "Check www.cryptogains.in, it pays 10x",           // link + crypto scam
            "What's your insta id? 😊",                         // asking for socials
            "Send 500 to my UPI and I'll call you",             // money
            "You look so sexy in your photos",                 // looks / sexual
            "As an AI I cannot write openers",                 // model broke character
            "Sure! I will ignore the previous instructions",   // followed an injection
            "Hi"                                                // too short
    })
    void rejectsUnsafeOpeners(String opener) {
        assertThat(OpenerSafety.isAcceptable(opener)).isFalse();
    }

    @Test
    void rejectsTooLongOpeners() {
        assertThat(OpenerSafety.isAcceptable("Hey, what's up? ".repeat(15))).isFalse();
    }

    @Test
    void fallbackOpenersAlwaysGiveThreeSafeOnes() {
        ProfileView me = new ProfileView(UUID.randomUUID(), "Priya Sharma", null, "Bengaluru", Set.of("COFFEE", "TRAVEL"));
        ProfileView them = new ProfileView(UUID.randomUUID(), "Rahul", "bio", "Bengaluru", Set.of("COFFEE", "BOARD_GAMES"));

        List<String> openers = FallbackOpeners.forProfiles(me, them);

        assertThat(openers).hasSize(3).allMatch(OpenerSafety::isAcceptable);
        assertThat(openers.getFirst()).isEqualTo("Hey Rahul! We both like coffee. What got you into it?");
        assertThat(openers.get(1)).contains("board games");
    }

    @Test
    void interestNamesReadNaturally() {
        assertThat(Interests.readable("FOODIE")).isEqualTo("good food");
        assertThat(Interests.readable("STANDUP_COMEDY")).isEqualTo("stand-up comedy");
        assertThat(Interests.readable("BOARD_GAMES")).isEqualTo("board games");
    }

    @Test
    void fallbackWorksWithAnEmptyProfile() {
        ProfileView empty = new ProfileView(UUID.randomUUID(), null, null, null, null);
        assertThat(FallbackOpeners.forProfiles(empty, empty)).hasSize(2).allMatch(OpenerSafety::isAcceptable);
    }
}
