package org.sabha.analytics.applicationservice;

import java.util.UUID;

import org.sabha.common.NotFoundException;

/**
 * Raised when a Sant picks a City that does not exist (Slice 17). Maps to HTTP
 * 404 via the common {@link NotFoundException} handling.
 */
public class CityNotFoundException extends NotFoundException {

    public CityNotFoundException(UUID cityId) {
        super("City " + cityId + " does not exist");
    }
}
