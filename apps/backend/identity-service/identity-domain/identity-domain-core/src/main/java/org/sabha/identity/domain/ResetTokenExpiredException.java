package org.sabha.identity.domain;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * The reset token was presented to {@code complete} after its
 * {@link PasswordReset#RESET_TOKEN_TTL} window elapsed (ADR-0004). The password
 * is unchanged; the User must restart the reset. Mapped to HTTP 422.
 */
public class ResetTokenExpiredException extends DomainException {

    public ResetTokenExpiredException(UUID resetId) {
        super("Reset token for reset " + resetId + " has expired");
    }
}
