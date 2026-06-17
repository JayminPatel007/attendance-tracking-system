package org.sabha.identity.applicationservice.sabhadefinition;

import java.util.UUID;

import org.sabha.common.NotFoundException;

/**
 * Thrown when a Sabha is defined against a {@code sabhaKindId} that names no
 * registered Sabha Kind (ADR-0012). Mapped to HTTP 404.
 */
public class SabhaKindNotFoundException extends NotFoundException {

    public SabhaKindNotFoundException(UUID sabhaKindId) {
        super("No Sabha Kind " + sabhaKindId);
    }
}
