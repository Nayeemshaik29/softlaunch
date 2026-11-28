package com.softlaunch.match.service;

import com.softlaunch.match.client.UserClient;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

/** Pays for a "note with a like" from the user's ❤ wallet (user-service). */
@Component
public class HeartSpender {

    private final UserClient users;

    public HeartSpender(UserClient users) {
        this.users = users;
    }

    public void spendNote(UUID me, UUID target) {
        try {
            users.spendHearts(me, Map.of("item", "NOTE", "ref", target.toString()));
        } catch (FeignException.FeignClientException e) {
            if (e.status() == 402) throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "A note costs 5 ❤. Get Hearts to send one.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That note couldn't be sent.");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Notes are unavailable right now. Try again.");
        }
    }
}
