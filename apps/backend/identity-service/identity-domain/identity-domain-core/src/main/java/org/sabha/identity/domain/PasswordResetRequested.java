package org.sabha.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEvent;

/**
 * Audit signal (ADR-0004): a self-service password reset was opened for
 * {@code userId}. Recorded for the audit trail; the password has not changed yet
 * and the OTP code itself is never carried on the event.
 *
 * @param aggregateId the new reset's id
 * @param userId      the {@code users.id} whose password is being reset
 */
public record PasswordResetRequested(
        UUID aggregateId,
        UUID userId,
        Instant occurredAt) implements DomainEvent {
}
