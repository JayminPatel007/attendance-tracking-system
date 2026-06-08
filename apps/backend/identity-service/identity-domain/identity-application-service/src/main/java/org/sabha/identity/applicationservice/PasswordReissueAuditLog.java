package org.sabha.identity.applicationservice;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit sink for assigner-reissue acts (ADR-0004): an appointing
 * Karyakar (or an MK member, for Sants) reset {@code targetUserId}'s password
 * out-of-band. Distinct from {@link PasswordResetRepository} — a reissue has no
 * OTP aggregate to load or save, only an actor and a timestamp to record. The
 * JDBC adapter writes it into the shared {@code password_resets} table so Slice
 * 19 reads both reset paths uniformly.
 */
public interface PasswordReissueAuditLog {

    void recordReissue(UUID id, UUID targetUserId, UUID actorUserId, Instant reissuedAt);
}
