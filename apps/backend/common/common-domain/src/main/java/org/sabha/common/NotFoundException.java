package org.sabha.common;

/**
 * Base for "the thing you asked for does not exist" domain exceptions
 * (ADR-0020). Mapped to HTTP 404 by the global exception handler.
 */
public abstract class NotFoundException extends DomainException {

    protected NotFoundException(String message) {
        super(message);
    }
}
