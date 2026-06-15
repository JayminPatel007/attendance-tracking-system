package org.sabha.identity.applicationservice;

import org.sabha.common.ConflictException;

/**
 * Raised when an appointment would create a User with a username that already
 * exists (ADR-0011). Surfaced as HTTP 409 so the appointment form can offer a
 * different username before re-submitting; nothing is committed.
 */
public class UsernameAlreadyTakenException extends ConflictException {

    private final String username;

    public UsernameAlreadyTakenException(String username) {
        super("Username already taken: " + username, "USERNAME_TAKEN");
        this.username = username;
    }

    public String username() {
        return username;
    }
}
