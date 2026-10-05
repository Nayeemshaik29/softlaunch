package com.softlaunch.user.dto;

import java.util.List;
import java.util.Map;

public record ProfileOptionsResponse(
        List<OptionDto> genders,
        List<OptionDto> relationshipIntents,
        List<OptionDto> interests,
        List<OptionDto> habits,
        Map<String, Integer> limits,
        List<String> suggestedPrompts
) {
}