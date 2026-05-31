package org.sabha.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEvent;

/**
 * Audit signal (ADR-0002): the Person entered the correct OTP within its TTL,
 * giving verified consent. The Roster swap follows in the same transaction.
 *
 * @param aggregateId the transfer that was confirmed
 * @param personId    the consenting Person
 */
public record TransferOtpConfirmed(
        UUID aggregateId,
        UUID personId,
        Instant occurredAt) implements DomainEvent {
}
