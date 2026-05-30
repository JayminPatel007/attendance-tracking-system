package org.sabha.identity.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.sabha.common.DomainEvent;

/**
 * Audit signal (ADR-0013): the adder chose "none of these — create new" despite
 * a non-empty name candidate list. Records who overrode, which candidates they
 * were shown, and the Person they created. A high override rate hints the name
 * matcher is too aggressive.
 *
 * @param aggregateId      the newly created Person's id
 * @param adderUserId      the {@code users.id} that performed the add
 * @param candidateIdsShown the candidate Person ids displayed at override time
 */
public record PersonAddedOverDuplicateWarning(
        UUID aggregateId,
        UUID adderUserId,
        List<UUID> candidateIdsShown,
        Instant occurredAt) implements DomainEvent {
}
