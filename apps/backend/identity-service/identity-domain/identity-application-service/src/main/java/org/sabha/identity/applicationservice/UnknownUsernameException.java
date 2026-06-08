package org.sabha.identity.applicationservice;

import org.sabha.common.NotFoundException;

/**
 * No User holds the given login name. Raised by the unauthenticated
 * password-reset and "who appointed me" lookups (ADR-0004), which resolve by
 * username; revealing the miss is the chosen UX over a generic response. Mapped
 * to HTTP 404.
 */
public class UnknownUsernameException extends NotFoundException {

    public UnknownUsernameException(String username) {
        super("No user with username '" + username + "'");
    }
}
