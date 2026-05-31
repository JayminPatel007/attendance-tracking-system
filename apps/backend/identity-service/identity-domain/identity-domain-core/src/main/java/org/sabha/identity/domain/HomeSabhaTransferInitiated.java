package org.sabha.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEvent;

/**
 * Audit signal (ADR-0002): a Sanchalak/Sah-Sanchalak began a Verified Home Sabha
 * Transfer, pulling {@code personId} toward {@code destinationSabhaId}. Records
 * who initiated for the audit trail; the swap has not happened yet.
 *
 * @param aggregateId        the new transfer's id
 * @param personId           the Person being transferred
 * @param destinationSabhaId the Sabha they would join
 * @param initiatingUserId   the {@code users.id} that initiated
 */
public record HomeSabhaTransferInitiated(
        UUID aggregateId,
        UUID personId,
        UUID destinationSabhaId,
        UUID initiatingUserId,
        Instant occurredAt) implements DomainEvent {
}
