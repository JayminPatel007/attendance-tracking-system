package org.sabha.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEvent;

/**
 * Audit signal (ADR-0002): the Roster swap committed — the Person's Home Sabha
 * for the affected demographic moved from {@code previousSabhaId} to
 * {@code destinationSabhaId}. Their other-demographic Home Sabhas are unchanged.
 *
 * @param aggregateId        the transfer that drove the swap
 * @param personId           the transferred Person
 * @param previousSabhaId    the Home Sabha they left
 * @param destinationSabhaId the Home Sabha they joined
 */
public record HomeSabhaSwapped(
        UUID aggregateId,
        UUID personId,
        UUID previousSabhaId,
        UUID destinationSabhaId,
        Instant occurredAt) implements DomainEvent {
}
