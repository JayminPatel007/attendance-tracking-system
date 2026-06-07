package org.sabha.analytics.applicationservice;

import java.util.UUID;

/**
 * Raised when a non-Sant tries to pick a dashboard City (Slice 17). Only a Sant
 * has the universal-read exception and therefore the City chip; every other role
 * is locked to their role-scoped view. Maps to HTTP 403.
 */
public class NotASantException extends RuntimeException {

    public NotASantException(UUID userId) {
        super("User " + userId + " is not a Sant and cannot choose a dashboard City");
    }
}
