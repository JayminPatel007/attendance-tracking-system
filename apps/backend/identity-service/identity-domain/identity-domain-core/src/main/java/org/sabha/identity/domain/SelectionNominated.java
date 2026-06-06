package org.sabha.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEvent;

/**
 * Audit signal (ADR-0006): a Regular Sanchalak nominated {@code personId} for the
 * selective ({@code BSS}/{@code YSS}) Sabha {@code selectiveSabhaId}. Records who
 * nominated for the audit trail; the Person does not yet gain the selective Home
 * Sabha — that waits on the demographic Nirdeshak's approval.
 *
 * @param aggregateId       the new nomination's id
 * @param personId          the Person being nominated
 * @param selectiveSabhaId  the BSS/YSS Sabha they would additionally join
 * @param nominatedBy       the {@code users.id} of the nominating Sanchalak
 */
public record SelectionNominated(
        UUID aggregateId,
        UUID personId,
        UUID selectiveSabhaId,
        UUID nominatedBy,
        Instant occurredAt) implements DomainEvent {
}
