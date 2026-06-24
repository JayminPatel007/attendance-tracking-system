package org.sabha.sabha.domain;

import java.util.UUID;

import org.sabha.common.NotFoundException;

/** Thrown when a Sabha referenced for deletion does not exist. HTTP 404. */
public class SabhaNotFoundException extends NotFoundException {

    public SabhaNotFoundException(UUID sabhaId) {
        super("No Sabha with id " + sabhaId);
    }
}
