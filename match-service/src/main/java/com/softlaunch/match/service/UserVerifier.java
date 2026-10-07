package com.softlaunch.match.service;

import com.softlaunch.match.client.UserClient;
import com.softlaunch.match.exception.TargetUserNotFoundException;
import com.softlaunch.match.exception.UserServiceUnavailableException;
import feign.FeignException;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserVerifier {

    private final UserClient userClient;
    private final CircuitBreaker circuitBreaker;

    public UserVerifier(UserClient userClient, CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.userClient = userClient;
        this.circuitBreaker = circuitBreakerFactory.create("user-service");
    }

    public void requireExists(UUID userId) {
        boolean exists = circuitBreaker.run(
                () -> {
                    try {
                        userClient.getUser(userId);
                        return true;
                    } catch (FeignException.NotFound notFound) {
                        return false;
                    }
                },
                failure -> {
                    throw new UserServiceUnavailableException(failure);
                }
        );

        if (!exists) {
            throw new TargetUserNotFoundException(userId);
        }
    }
}