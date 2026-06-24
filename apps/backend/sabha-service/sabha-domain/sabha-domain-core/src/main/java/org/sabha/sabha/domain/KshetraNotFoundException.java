package org.sabha.sabha.domain;

import java.util.UUID;

import org.sabha.common.NotFoundException;

/** Thrown when a Kshetra referenced for deletion does not exist. HTTP 404. */
public class KshetraNotFoundException extends NotFoundException {

    public KshetraNotFoundException(UUID kshetraId) {
        super("No Kshetra with id " + kshetraId);
    }
}
