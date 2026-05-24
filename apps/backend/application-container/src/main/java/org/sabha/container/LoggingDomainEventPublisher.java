package org.sabha.container;

import java.util.List;

import org.sabha.common.DomainEvent;
import org.sabha.common.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-process {@link DomainEventPublisher} that logs each event at INFO and
 * does nothing else (ADR-0019, ADR-0020). Real subscribers — analytics
 * projections, cross-context flows — wire in by listening to a richer
 * implementation that lands in a later sub-PR. For now this gives application
 * services a non-null dependency so the four-step skeleton (load → mutate →
 * save → publish) is exercisable end-to-end.
 */
@Component
public class LoggingDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingDomainEventPublisher.class);

    @Override
    public void publishAll(List<? extends DomainEvent> events) {
        for (DomainEvent event : events) {
            log.info("domain event: {} aggregateId={} occurredAt={}",
                    event.getClass().getSimpleName(),
                    event.aggregateId(),
                    event.occurredAt());
        }
    }
}
