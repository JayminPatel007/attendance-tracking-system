package org.sabha.identity.applicationservice;

import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.domain.HomeSabhaTransfer;

/**
 * Driven port persisting {@link HomeSabhaTransfer} aggregates (ADR-0002). As an
 * {@link OtpSendLog} it also backs the per-mobile OTP rate limit (3 sends/hour)
 * and resend cooldown (30s) from PRD-0001.
 */
public interface HomeSabhaTransferRepository extends OtpSendLog {

    void save(HomeSabhaTransfer transfer);

    Optional<HomeSabhaTransfer> findById(UUID transferId);
}
