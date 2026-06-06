package org.sabha.identity.applicationservice;

import java.util.UUID;

/**
 * A {@code PENDING} nomination already exists for this Person on this selective
 * track (ADR-0006) — a second one would clutter the Nirdeshak's queue with a
 * duplicate decision. A transport-tier signal mapped to HTTP 409.
 */
public class DuplicateNominationException extends RuntimeException {

    public DuplicateNominationException(UUID personId, String track) {
        super("A pending " + track + " nomination already exists for Person " + personId);
    }
}
