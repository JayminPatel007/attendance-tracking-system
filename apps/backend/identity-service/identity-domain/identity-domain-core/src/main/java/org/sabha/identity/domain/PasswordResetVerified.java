package org.sabha.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEvent;

/**
 * Audit signal (ADR-0004): the reset OTP was entered correctly and a short-lived
 * reset token was issued. The password has not changed yet — that happens on
 * {@code complete}.
 *
 * @param aggregateId the reset that was verified
 */
public record PasswordResetVerified(
        UUID aggregateId,
        Instant occurredAt) implements DomainEvent {
}
