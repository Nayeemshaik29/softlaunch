package com.softlaunch.user.desires;

import com.softlaunch.user.model.Desire;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

/** PUT body and GET response for /api/users/me/profile/desires (only the owner ever sees NOT_INTO). */
public record DesiresDto(
        @NotNull @Size(max = 3, message = "Pick up to 3 you're really into") Set<Desire> into,
        @NotNull @Size(max = 3, message = "Pick up to 3 you're open to") Set<Desire> openTo,
        @NotNull Set<Desire> notInto) {

    @AssertTrue(message = "Each desire can only be in one list")
    public boolean isDisjoint() {
        if (into == null || openTo == null || notInto == null) return true;
        Set<Desire> all = new HashSet<>();
        return into.stream().allMatch(all::add) && openTo.stream().allMatch(all::add) && notInto.stream().allMatch(all::add);
    }
}
