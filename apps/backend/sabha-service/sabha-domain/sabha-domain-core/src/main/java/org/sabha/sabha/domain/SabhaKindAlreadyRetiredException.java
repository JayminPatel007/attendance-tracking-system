package org.sabha.sabha.domain;

import java.util.UUID;

import org.sabha.common.ConflictException;

/**
 * Thrown when an already-retired {@link SabhaKind} is retired again (ADR-0026).
 * A state collision rather than malformed input, so HTTP 409.
 */
public class SabhaKindAlreadyRetiredException extends ConflictException {

    public SabhaKindAlreadyRetiredException(UUID kindId) {
        super("Sabha Kind " + kindId + " is already retired", "SABHA_KIND_ALREADY_RETIRED");
    }
}
