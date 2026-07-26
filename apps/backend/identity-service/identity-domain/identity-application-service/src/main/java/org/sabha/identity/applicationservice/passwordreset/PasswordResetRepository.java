package org.sabha.identity.applicationservice.passwordreset;

import java.util.Optional;

import org.sabha.identity.applicationservice.otp.OtpGuardedRepository;
import org.sabha.identity.domain.PasswordReset;

/**
 * Driven port persisting {@link PasswordReset} aggregates (ADR-0004). The JDBC
 * adapter lives in {@code identity-data-access}; unit tests drive an in-memory
 * fake. As an {@link OtpGuardedRepository} it gives
 * {@link org.sabha.identity.applicationservice.otp.OtpGuardedFlow OtpGuardedFlow}
 * the upsert-and-load it drives this flow through, and backs the reset OTP rate
 * limit and resend cooldown (PRD-0001); the lookup below is this flow's own.
 */
public interface PasswordResetRepository extends OtpGuardedRepository<PasswordReset> {

    /** The reset holding the given verified reset token, if any — drives {@code complete}. */
    Optional<PasswordReset> findByResetToken(String resetToken);
}
