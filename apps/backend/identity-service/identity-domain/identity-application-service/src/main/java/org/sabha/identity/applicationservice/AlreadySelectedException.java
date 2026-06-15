package org.sabha.identity.applicationservice;

import java.util.UUID;

import org.sabha.common.ConflictException;

/**
 * The Person is already on the selective Sabha's Roster (ADR-0006) — they hold
 * the selective Home Sabha, so there is nothing to nominate. A transport-tier
 * signal mapped to HTTP 409.
 */
public class AlreadySelectedException extends ConflictException {

    public AlreadySelectedException(UUID personId, UUID selectiveSabhaId) {
        super("Person " + personId + " is already on the Roster of selective Sabha "
                + selectiveSabhaId);
    }
}
