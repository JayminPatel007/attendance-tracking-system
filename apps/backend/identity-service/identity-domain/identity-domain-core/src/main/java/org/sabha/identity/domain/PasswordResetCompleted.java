package org.sabha.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEvent;

/**
 * Audit signal (ADR-0004): a self-service password reset completed — the User's
 * credential was changed at the identity provider. Carries {@code userId} so the
 * audit trail attributes the reset to its subject (who is also the actor on the
 * self-service path).
 *
 * @param aggregateId the reset that completed
 * @param userId      the {@code users.id} whose password was changed
 */
public record PasswordResetCompleted(
        UUID aggregateId,
        UUID userId,
        Instant occurredAt) implements DomainEvent {
}
