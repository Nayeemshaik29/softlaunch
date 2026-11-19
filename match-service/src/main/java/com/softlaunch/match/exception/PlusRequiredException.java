package com.softlaunch.match.exception;

/** A Plus perk was used without Plus. The app shows the Plus screen (problem code PLUS_REQUIRED). */
public class PlusRequiredException extends RuntimeException {
    public PlusRequiredException(String what) {
        super(what + " is part of SoftLaunch Plus.");
    }
}
