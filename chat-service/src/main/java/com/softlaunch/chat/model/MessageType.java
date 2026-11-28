package com.softlaunch.chat.model;

/**
 * TEXT = written by a person. SYSTEM = written by the app, e.g. "took a screenshot".
 * STICKER = a free flirty sticker, GIFT = a paid animated gift (3 ❤); for both, text holds the sticker id.
 */
public enum MessageType { TEXT, SYSTEM, STICKER, GIFT }
