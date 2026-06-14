package org.sabha.identity.applicationservice;

import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.domain.PasswordReset;

/**
 * Driven port persisting {@link PasswordReset} aggregates (ADR-0004). The JDBC
 * adapter lives in {@code identity-data-access}; unit tests drive an in-memory
 * fake. {@code save} is an upsert keyed on the reset id, mirroring
 * {@link HomeSabhaTransferRepository}. As an {@link OtpSendLog} it also backs the
 * reset OTP rate limit and resend cooldown (PRD-0001).
 */
public interface PasswordResetRepository extends OtpSendLog {

    void save(PasswordReset reset);

    Optional<PasswordReset> findById(UUID id);

    /** The reset holding the given verified reset token, if any — drives {@code complete}. */
    Optional<PasswordReset> findByResetToken(String resetToken);
}
