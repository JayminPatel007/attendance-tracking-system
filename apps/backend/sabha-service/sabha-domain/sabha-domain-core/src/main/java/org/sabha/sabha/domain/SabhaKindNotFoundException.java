package org.sabha.sabha.domain;

import java.util.UUID;

import org.sabha.common.NotFoundException;

/** Thrown when a retire/reactivate targets a Sabha Kind that does not exist. HTTP 404. */
public class SabhaKindNotFoundException extends NotFoundException {

    public SabhaKindNotFoundException(UUID kindId) {
        super("No Sabha Kind with id " + kindId);
    }
}
