package org.sabha.attendance.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEvent;

public record AttendanceMarked(
        UUID aggregateId,
        UUID personId,
        boolean present,
        UUID markedBy,
        Instant occurredAt) implements DomainEvent {
}
