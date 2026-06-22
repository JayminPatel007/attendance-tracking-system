package org.sabha.sabha.domain;

import java.util.UUID;

import org.sabha.common.ConflictException;

/**
 * Thrown when an active {@link SabhaKind} is reactivated — there is nothing to
 * reactivate (ADR-0026). A state collision rather than malformed input, so HTTP 409.
 */
public class SabhaKindNotRetiredException extends ConflictException {

    public SabhaKindNotRetiredException(UUID kindId) {
        super("Sabha Kind " + kindId + " is not retired", "SABHA_KIND_NOT_RETIRED");
    }
}
