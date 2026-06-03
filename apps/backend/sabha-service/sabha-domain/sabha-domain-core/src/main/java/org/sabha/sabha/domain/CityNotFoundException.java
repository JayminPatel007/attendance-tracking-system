package org.sabha.sabha.domain;

import java.util.UUID;

import org.sabha.common.NotFoundException;

/** Thrown when a Zone is created under a City that does not exist. HTTP 404. */
public class CityNotFoundException extends NotFoundException {

    public CityNotFoundException(UUID cityId) {
        super("No City with id " + cityId);
    }
}
