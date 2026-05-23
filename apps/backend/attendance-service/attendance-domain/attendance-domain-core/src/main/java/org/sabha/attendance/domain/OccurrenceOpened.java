package org.sabha.attendance.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEvent;

public record OccurrenceOpened(UUID aggregateId, Instant occurredAt) implements DomainEvent {
}
