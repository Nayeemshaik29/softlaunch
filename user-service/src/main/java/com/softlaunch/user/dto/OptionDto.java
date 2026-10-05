package com.softlaunch.user.dto;

public record OptionDto(String value, String label) {

    public static OptionDto of(Enum<?> option) {
        String words = option.name().replace('_', ' ').toLowerCase();
        String label = Character.toUpperCase(words.charAt(0)) + words.substring(1);
        return new OptionDto(option.name(), label);
    }
}