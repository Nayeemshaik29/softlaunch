package com.softlaunch.user.controller;

import com.softlaunch.user.dto.UserResponse;
import com.softlaunch.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(@RequestHeader("X-User-Id") UUID userId) {
        return userService.getById(userId);
    }
}