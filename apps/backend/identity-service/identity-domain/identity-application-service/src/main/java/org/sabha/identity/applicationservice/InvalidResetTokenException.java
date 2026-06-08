package org.sabha.identity.applicationservice;

import org.sabha.common.NotFoundException;

/**
 * The reset token presented to {@code complete} matches no reset (ADR-0004) —
 * it was never issued, or belongs to a reset that was never OTP-verified (a
 * {@code PENDING} reset holds no token). Mapped to HTTP 404.
 */
public class InvalidResetTokenException extends NotFoundException {

    public InvalidResetTokenException() {
        super("No password reset matches the supplied reset token");
    }
}
