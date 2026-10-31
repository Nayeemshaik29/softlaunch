package com.softlaunch.chat.dto;

public record TypingEvent(String userId, boolean typing) {
}