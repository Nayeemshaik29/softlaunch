package com.softlaunch.user.controller;

import com.softlaunch.user.deletion.AccountDeletionService;
import com.softlaunch.user.deletion.DeleteAccountRequest;
import com.softlaunch.user.dto.UserResponse;
import com.softlaunch.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AccountDeletionService accountDeletion;

    public UserController(UserService userService, AccountDeletionService accountDeletion) {
        this.userService = userService;
        this.accountDeletion = accountDeletion;
    }

    @GetMapping("/me")
    public UserResponse me(@RequestHeader("X-User-Id") UUID userId) {
        return userService.getById(userId);
    }

    /** Permanently deletes the account and (through user.deleted) the user's data in every service. */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(@RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody DeleteAccountRequest request) {
        accountDeletion.deleteAccount(userId, request.password());
    }
}
