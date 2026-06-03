package org.sabha.attendance.applicationservice;

import java.util.UUID;

import org.sabha.common.NotFoundException;

/**
 * Thrown when an Occurrence is created against a {@code sabhaId} that names no
 * Sabha (ADR-0012). Mapped to HTTP 404.
 */
public class SabhaNotFoundException extends NotFoundException {

    public SabhaNotFoundException(UUID sabhaId) {
        super("No Sabha " + sabhaId);
    }
}
