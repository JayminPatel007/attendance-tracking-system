package org.sabha.identity.applicationservice.selection;

import java.util.UUID;

import org.sabha.common.AuthorizationDeniedException;

/**
 * Raised when a user who is neither Sanchalak nor Sah-Sanchalak of the Regular
 * Sabha tries to nominate one of its Roster Persons for the selective BSS/YSS
 * track (ADR-0006). A transport-tier signal — sibling of the domain exceptions —
 * mapped to HTTP 403.
 */
public class NominationNotAuthorizedException extends AuthorizationDeniedException {

    public NominationNotAuthorizedException(UUID userId, UUID regularSabhaId) {
        super("User " + userId + " is not the Sanchalak of Sabha " + regularSabhaId
                + " and may not nominate from its Roster");
    }
}
