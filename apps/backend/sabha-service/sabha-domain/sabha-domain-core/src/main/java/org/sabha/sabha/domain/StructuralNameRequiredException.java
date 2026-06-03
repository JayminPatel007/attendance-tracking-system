package org.sabha.sabha.domain;

import org.sabha.common.DomainException;

/**
 * Thrown when a structural entity (City, Zone, Kshetra) is created without a
 * non-blank name (ADR-0009). Mapped to HTTP 422.
 */
public class StructuralNameRequiredException extends DomainException {

    public StructuralNameRequiredException(String entity) {
        super("A " + entity + " must have a non-blank name");
    }
}
