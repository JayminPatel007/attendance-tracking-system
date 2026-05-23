package org.sabha.common;

import java.util.UUID;

/**
 * Thrown by a repository's {@code save} when the aggregate's version on disk
 * has changed since the application service loaded it (ADR-0020). The
 * application service catches this and retries the load-mutate-save cycle a
 * bounded number of times before surfacing a
 * {@link ConcurrentModificationException} to the caller.
 *
 * <p>Not a {@link DomainException} — this is a transaction-level conflict, not
 * a domain rule violation. The global exception handler does not map it
 * directly; callers should always catch and retry.
 */
public class OptimisticLockException extends RuntimeException {

    private final UUID aggregateId;

    public OptimisticLockException(UUID aggregateId) {
        super("Optimistic lock conflict on aggregate " + aggregateId);
        this.aggregateId = aggregateId;
    }

    public UUID aggregateId() {
        return aggregateId;
    }
}
