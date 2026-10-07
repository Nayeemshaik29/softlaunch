package com.softlaunch.user.controller;

import com.softlaunch.user.dto.UserSummary;
import com.softlaunch.user.exception.UserNotFoundException;
import com.softlaunch.user.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserRepository userRepository;

    public InternalUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/{userId}")
    public UserSummary getUser(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(user -> new UserSummary(user.getId(), user.getDisplayName()))
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}