package org.sabha.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEvent;

/**
 * Audit signal (ADR-0006): the demographic Nirdeshak deselected {@code personId},
 * removing the selective Sabha {@code selectiveSabhaId} from their Home Sabhas.
 * The Regular Home Sabha is untouched — deselection is the inverse of approval.
 * Records who revoked for the audit trail.
 *
 * @param aggregateId      the deselected nomination's id
 * @param personId         the Person removed from the selective Roster
 * @param selectiveSabhaId the BSS/YSS Sabha they left
 * @param revokedBy        the {@code users.id} of the deselecting Nirdeshak
 */
public record SelectionRevoked(
        UUID aggregateId,
        UUID personId,
        UUID selectiveSabhaId,
        UUID revokedBy,
        Instant occurredAt) implements DomainEvent {
}
