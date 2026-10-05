package com.softlaunch.user.dto;

import com.softlaunch.user.validation.Adult;
import jakarta.validation.constraints.*;
import com.softlaunch.user.validation.Adult;

import java.time.LocalDate;

public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 50) String displayName,
        @NotNull @Past @Adult LocalDate dateOfBirth
)
 {
}