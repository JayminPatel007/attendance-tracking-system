package org.sabha.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEvent;

/**
 * Audit signal (ADR-0006): the demographic Nirdeshak approved a nomination, so
 * {@code personId} additionally joins the selective Sabha {@code selectiveSabhaId}.
 * Their Regular Home Sabha is untouched — selection is additive. Records who
 * approved for the audit trail.
 *
 * @param aggregateId      the nomination's id
 * @param personId         the Person now on the selective Roster
 * @param selectiveSabhaId the BSS/YSS Sabha they joined
 * @param approvedBy       the {@code users.id} of the approving Nirdeshak
 */
public record SelectionApproved(
        UUID aggregateId,
        UUID personId,
        UUID selectiveSabhaId,
        UUID approvedBy,
        Instant occurredAt) implements DomainEvent {
}
