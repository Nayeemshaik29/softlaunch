package com.softlaunch.user.dto;

import com.softlaunch.user.model.Prompt;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PromptDto(
        @NotBlank @Size(max = 100) String question,
        @NotBlank @Size(max = 300) String answer
) {
    public static PromptDto from(Prompt prompt) {
        return new PromptDto(prompt.getQuestion(), prompt.getAnswer());
    }

    public Prompt toEntity() {
        return new Prompt(question, answer);
    }
}