package org.sabha.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEvent;

/**
 * Audit signal (ADR-0004): the reset OTP was dispatched to the User's registered
 * mobile. The code itself is never recorded.
 *
 * @param aggregateId the reset the OTP belongs to
 */
public record PasswordResetOtpSent(
        UUID aggregateId,
        Instant occurredAt) implements DomainEvent {
}
