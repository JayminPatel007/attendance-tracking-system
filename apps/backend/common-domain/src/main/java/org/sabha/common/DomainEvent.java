package org.sabha.common;

import java.time.Instant;
import java.util.UUID;

/**
 * Something that has happened in the domain. Emitted by aggregate roots during
 * state-mutating methods and dispatched by {@link DomainEventPublisher} after
 * the aggregate is saved (ADR-0020).
 *
 * <p>Concrete events live in their owning context's {@code *-domain-core}
 * module (e.g. {@code OccurrenceOpened} in {@code attendance-domain-core}).
 */
public interface DomainEvent {

    Instant occurredAt();

    UUID aggregateId();
}
