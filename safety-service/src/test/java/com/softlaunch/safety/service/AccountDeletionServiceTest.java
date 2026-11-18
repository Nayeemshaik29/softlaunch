package com.softlaunch.safety.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountDeletionServiceTest {

    @Test
    void pseudonymIsStableSoReporterCountsStayCorrect() {
        UUID user = UUID.randomUUID();
        assertThat(AccountDeletionService.pseudonym(user)).isEqualTo(AccountDeletionService.pseudonym(user));
    }

    @Test
    void pseudonymIsNotTheRealIdAndDiffersPerUser() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertThat(AccountDeletionService.pseudonym(a)).isNotEqualTo(a.toString())
                .isNotEqualTo(AccountDeletionService.pseudonym(b));
    }

    @Test
    void pseudonymIsStillAValidUuidForModeratorScreens() {
        String alias = AccountDeletionService.pseudonym(UUID.randomUUID());
        assertThat(UUID.fromString(alias).version()).isEqualTo(3);
    }
}
