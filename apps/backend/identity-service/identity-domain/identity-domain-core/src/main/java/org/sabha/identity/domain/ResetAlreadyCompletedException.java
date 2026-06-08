package org.sabha.identity.domain;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * {@code complete} was called on a reset that already completed (ADR-0004) — a
 * replay of a consumed reset token. The password is unchanged. Mapped to HTTP 422.
 */
public class ResetAlreadyCompletedException extends DomainException {

    public ResetAlreadyCompletedException(UUID resetId) {
        super("Password reset " + resetId + " has already been completed");
    }
}
