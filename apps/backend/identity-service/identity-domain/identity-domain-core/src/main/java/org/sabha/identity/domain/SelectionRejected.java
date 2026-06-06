package org.sabha.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEvent;

/**
 * Audit signal (ADR-0006): the demographic Nirdeshak rejected a nomination, so
 * {@code personId} does not join the selective Sabha. Records who rejected and
 * any reason for the audit trail.
 *
 * @param aggregateId the nomination's id
 * @param personId    the Person who was not selected
 * @param rejectedBy  the {@code users.id} of the rejecting Nirdeshak
 * @param reason      the optional rejection reason ({@code null} if none given)
 */
public record SelectionRejected(
        UUID aggregateId,
        UUID personId,
        UUID rejectedBy,
        String reason,
        Instant occurredAt) implements DomainEvent {
}
