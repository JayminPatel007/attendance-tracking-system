package org.sabha.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEvent;

/**
 * Audit signal (ADR-0002): the consent OTP for a Verified Home Sabha Transfer was
 * dispatched to the Person's registered mobile. The code itself is never recorded.
 *
 * @param aggregateId the transfer the OTP belongs to
 */
public record TransferOtpSent(
        UUID aggregateId,
        Instant occurredAt) implements DomainEvent {
}
