package org.sabha.common;

import java.util.List;

/**
 * Driven port for publishing domain events after an aggregate has been saved
 * (ADR-0020). The in-process implementation lives in
 * {@code application-container}; later slices may swap in an out-of-process
 * bus (e.g. Kafka) without changing application-service callers.
 *
 * <p>Application services call {@code publishAll(occurrence.pullDomainEvents())}
 * as the final step of the load → mutate → save → publish skeleton.
 */
public interface DomainEventPublisher {

    void publishAll(List<? extends DomainEvent> events);
}
