package org.sabha.common;

/**
 * Base type for domain rule violations (ADR-0020). Subclasses are mapped to
 * HTTP 422 by the global exception handler in {@code application-container};
 * more specific subclasses (e.g. {@link NotFoundException}) override that
 * mapping.
 *
 * <p>Throw from aggregate methods or domain services when a precondition or
 * invariant is violated. Do <em>not</em> throw from application services for
 * orchestration concerns — those use
 * {@link ConcurrentModificationException} or specific subclasses instead.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
