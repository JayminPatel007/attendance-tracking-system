package org.sabha.sabha.domain;

import org.sabha.common.DomainException;

/**
 * Thrown when a {@code (demographic, track)} kind is registered that already
 * exists — the set of kinds is unique on that pair (ADR-0009). HTTP 422.
 */
public class SabhaKindAlreadyRegisteredException extends DomainException {

    public SabhaKindAlreadyRegisteredException(Demographic demographic, Track track) {
        super("Sabha Kind " + demographic + " (" + track + ") is already registered");
    }
}
