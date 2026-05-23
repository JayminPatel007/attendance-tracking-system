package org.sabha.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AggregateRootTest {

    @Test
    void pullDomainEvents_returns_registered_events_then_clears_them() {
        TestAggregate aggregate = new TestAggregate(UUID.randomUUID());
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();
        aggregate.doSomething(eventId1);
        aggregate.doSomething(eventId2);

        // first pull: returns both events in registration order
        assertThat(aggregate.pullDomainEvents())
                .extracting(DomainEvent::aggregateId)
                .containsExactly(eventId1, eventId2);

        // second pull: list is drained
        assertThat(aggregate.pullDomainEvents()).isEmpty();
    }

    private static final class TestAggregate extends AggregateRoot<UUID> {

        private final UUID id;

        TestAggregate(UUID id) {
            this.id = id;
        }

        void doSomething(UUID eventAggregateId) {
            registerEvent(new TestEvent(eventAggregateId, Instant.now()));
        }

        @Override
        public UUID id() {
            return id;
        }
    }

    private record TestEvent(UUID aggregateId, Instant occurredAt) implements DomainEvent {
    }
}
