package org.sabha.identity.applicationservice.transfer;

import org.sabha.identity.applicationservice.otp.OtpGuardedRepository;
import org.sabha.identity.domain.HomeSabhaTransfer;

/**
 * Driven port persisting {@link HomeSabhaTransfer} aggregates (ADR-0002). As an
 * {@link OtpGuardedRepository} it gives
 * {@link org.sabha.identity.applicationservice.otp.OtpGuardedFlow OtpGuardedFlow}
 * the upsert-and-load it drives this flow through, and backs the per-mobile OTP
 * rate limit (3 sends/hour) and resend cooldown (30s) from PRD-0001. The transfer
 * flow needs nothing beyond that.
 */
public interface HomeSabhaTransferRepository extends OtpGuardedRepository<HomeSabhaTransfer> {
}
