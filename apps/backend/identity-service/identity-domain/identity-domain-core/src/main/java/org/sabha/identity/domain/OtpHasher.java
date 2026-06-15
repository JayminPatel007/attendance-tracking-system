package org.sabha.identity.domain;

import java.util.UUID;

/**
 * Turns a plaintext OTP into the opaque digest that {@link OtpChallenge} holds and
 * persists, so a 6-digit code never sits in the database in the clear (issue #77).
 *
 * <p>The 6-digit space is trivially brute-forced offline against a bare digest, so
 * implementations must be <em>keyed</em> (a server-side secret) and are salted
 * per challenge by {@code challengeId}; the same code under two challenges yields
 * two different digests. {@link OtpChallenge#verify} compares the stored digest to
 * the digest of the candidate, so issue and verify must hash identically.</p>
 */
public interface OtpHasher {

    /** Returns the keyed, per-challenge-salted digest of {@code code}, safe to persist. */
    String hash(UUID challengeId, String code);
}
