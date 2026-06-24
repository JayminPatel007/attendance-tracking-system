package org.sabha.sabha.domain;

import java.util.UUID;

import org.sabha.common.NotFoundException;

/** Thrown when a Zone referenced for deletion does not exist. HTTP 404. */
public class ZoneNotFoundException extends NotFoundException {

    public ZoneNotFoundException(UUID zoneId) {
        super("No Zone with id " + zoneId);
    }
}
