package org.sabha.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Base type for DDD aggregate roots (ADR-0020).
 *
 * <p>Subclasses register {@link DomainEvent} instances during state-mutating
 * methods; the application service drains them via {@link #pullDomainEvents()}
 * after persisting the aggregate, then dispatches them through a
 * {@link DomainEventPublisher}.
 *
 * <p>The {@code version} field carries optimistic-locking state. Repositories
 * read it before save, write it back incremented; on row-version conflict the
 * adapter throws {@link OptimisticLockException}. The application service
 * retries the load-mutate-save cycle and gives up with a
 * {@link ConcurrentModificationException} after a bounded number of attempts.
 */
public abstract class AggregateRoot<ID> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();
    protected Long version;

    public abstract ID id();

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> drained = List.copyOf(domainEvents);
        domainEvents.clear();
        return drained;
    }

    public Long version() {
        return version;
    }
}
