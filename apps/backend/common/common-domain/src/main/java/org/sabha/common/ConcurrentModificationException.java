package org.sabha.common;

import java.util.UUID;

/**
 * Thrown by an application service when an aggregate could not be saved after
 * the bounded retry budget on {@link OptimisticLockException} was exhausted
 * (ADR-0020). Mapped to HTTP 409 by the global exception handler.
 *
 * <p>Distinct from {@link java.util.ConcurrentModificationException} — this
 * one is a domain-tier transport signal, not a collection-iteration error.
 * Always import this class with its fully-qualified package
 * ({@code org.sabha.common.ConcurrentModificationException}) at call sites.
 */
public class ConcurrentModificationException extends RuntimeException {

    private final UUID aggregateId;

    public ConcurrentModificationException(UUID aggregateId, Throwable cause) {
        super("Could not save aggregate " + aggregateId + " after retries", cause);
        this.aggregateId = aggregateId;
    }

    public UUID aggregateId() {
        return aggregateId;
    }
}
