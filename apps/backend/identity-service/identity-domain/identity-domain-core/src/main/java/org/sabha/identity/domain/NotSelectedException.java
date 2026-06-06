package org.sabha.identity.domain;

import java.util.UUID;

import org.sabha.common.NotFoundException;

/**
 * The Person has no approved selection into the given selective Sabha to revoke
 * (ADR-0006) — there is nothing to deselect. Mapped to HTTP 404.
 */
public class NotSelectedException extends NotFoundException {

    public NotSelectedException(UUID personId, UUID selectiveSabhaId) {
        super("Person " + personId + " has no approved selection into Sabha " + selectiveSabhaId);
    }
}
