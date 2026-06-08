package org.sabha.identity.applicationservice;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.domain.PasswordReset;

/**
 * Driven port persisting {@link PasswordReset} aggregates (ADR-0004). The JDBC
 * adapter lives in {@code identity-data-access}; unit tests drive an in-memory
 * fake. {@code save} is an upsert keyed on the reset id, mirroring
 * {@link HomeSabhaTransferRepository}.
 */
public interface PasswordResetRepository {

    void save(PasswordReset reset);

    Optional<PasswordReset> findById(UUID id);

    /** The reset holding the given verified reset token, if any — drives {@code complete}. */
    Optional<PasswordReset> findByResetToken(String resetToken);

    /** When the most recent reset OTP was sent to {@code mobile}, for the resend cooldown. */
    Optional<Instant> lastInitiatedAt(String mobile);

    /** How many reset OTPs were sent to {@code mobile} since {@code since}, for the rate limit. */
    int sendCountSince(String mobile, Instant since);
}
