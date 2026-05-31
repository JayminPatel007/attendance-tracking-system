package org.sabha.identity.applicationservice;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.domain.HomeSabhaTransfer;

/**
 * Driven port persisting {@link HomeSabhaTransfer} aggregates (ADR-0002). The
 * {@code sendCountSince} / {@code lastInitiatedAt} queries back the per-mobile OTP
 * rate limit (3 sends/hour) and resend cooldown (30s) from PRD-0001.
 */
public interface HomeSabhaTransferRepository {

    void save(HomeSabhaTransfer transfer);

    Optional<HomeSabhaTransfer> findById(UUID transferId);

    /** How many transfers (OTP sends) were initiated for {@code mobile} at or after {@code since}. */
    int sendCountSince(String mobile, Instant since);

    /** When the most recent transfer for {@code mobile} was initiated, if any. */
    Optional<Instant> lastInitiatedAt(String mobile);
}
