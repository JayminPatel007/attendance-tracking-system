package org.sabha.attendance.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEvent;

/**
 * A Finalized Occurrence was reopened for marking (ADR-0001). The grace period
 * has passed and a Kshetra-tier actor (Nirikshak / Nirdeshak / Sah-Nirdeshak)
 * judged a correction worth reopening for; the reopener, timestamp, and reason
 * are carried on the audit transition, not on this event.
 */
public record OccurrenceReopened(UUID aggregateId, Instant occurredAt) implements DomainEvent {
}
