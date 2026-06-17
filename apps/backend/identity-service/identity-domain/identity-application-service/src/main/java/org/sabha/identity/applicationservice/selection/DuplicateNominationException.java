package org.sabha.identity.applicationservice.selection;

import java.util.UUID;

import org.sabha.common.ConflictException;

/**
 * A {@code PENDING} nomination already exists for this Person on this selective
 * track (ADR-0006) — a second one would clutter the Nirdeshak's queue with a
 * duplicate decision. A transport-tier signal mapped to HTTP 409.
 */
public class DuplicateNominationException extends ConflictException {

    public DuplicateNominationException(UUID personId, String track) {
        super("A pending " + track + " nomination already exists for Person " + personId);
    }
}
