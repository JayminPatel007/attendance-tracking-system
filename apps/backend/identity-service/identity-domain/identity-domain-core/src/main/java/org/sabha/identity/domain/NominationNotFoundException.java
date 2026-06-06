package org.sabha.identity.domain;

import java.util.UUID;

import org.sabha.common.NotFoundException;

/**
 * No selection nomination exists with the given id (ADR-0006). Mapped to HTTP 404.
 */
public class NominationNotFoundException extends NotFoundException {

    public NominationNotFoundException(UUID nominationId) {
        super("No nomination with id " + nominationId);
    }
}
