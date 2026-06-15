package org.sabha.identity.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * The OTP consent state machine shared by OTP-gated flows (PRD-0001): a 6-digit
 * code with a 5-minute TTL and a 5-attempt budget after which it locks. Extracted
 * from {@link HomeSabhaTransfer}'s inline logic (Slice 8) so the self-service
 * {@link PasswordReset} (ADR-0004) reuses one tested implementation rather than
 * re-deriving it.
 *
 * <p>The owning aggregate holds one of these and delegates code entry to
 * {@link #verify}; the typed exceptions it raises carry the aggregate's id so
 * callers and audit see the right subject.</p>
 *
 * <p>The plaintext code never lives here: {@link #issue} stores only the
 * {@link OtpHasher} digest and {@link #verify} compares digests (issue #77), so a
 * read of the persisted {@code otp_code} column yields a hash, not a live code.</p>
 */
public final class OtpChallenge {

    /** OTP time-to-live (PRD-0001 Implementation Decisions). */
    public static final Duration TTL = Duration.ofMinutes(5);

    /** Wrong-OTP attempts allowed before the challenge locks (PRD-0001). */
    public static final int MAX_ATTEMPTS = 5;

    private final UUID challengeId;
    private final String codeHash;
    private final Instant expiresAt;
    private int attempts;
    private boolean locked;
    private boolean expired;

    private OtpChallenge(UUID challengeId, String codeHash, Instant expiresAt,
                         int attempts, boolean locked, boolean expired) {
        this.challengeId = challengeId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.attempts = attempts;
        this.locked = locked;
        this.expired = expired;
    }

    /**
     * Issues a fresh challenge valid for {@link #TTL} from {@code now}, storing only
     * the {@code hasher} digest of {@code code} — the plaintext is never retained.
     */
    public static OtpChallenge issue(UUID challengeId, String code, Instant now, OtpHasher hasher) {
        return new OtpChallenge(challengeId, hasher.hash(challengeId, code), now.plus(TTL), 0, false, false);
    }

    /**
     * Rebuilds a persisted challenge from its stored columns, registering nothing.
     * {@code codeHash} is the already-hashed value read from storage, so this does
     * <em>not</em> re-hash it.
     */
    public static OtpChallenge rehydrate(UUID challengeId, String codeHash, Instant expiresAt,
                                         int attempts, boolean locked, boolean expired) {
        return new OtpChallenge(challengeId, codeHash, expiresAt, attempts, locked, expired);
    }

    /**
     * Consumes a code attempt. Returns normally when {@code candidate} matches
     * within TTL; otherwise records the consequence (expiry / incremented attempt
     * / lockout) and throws the matching {@link org.sabha.common.DomainException}.
     * The candidate is hashed with the same {@code hasher} used at issue and the
     * digests are compared in constant time.
     */
    public void verify(String candidate, Instant now, OtpHasher hasher) {
        if (locked) {
            throw new OtpAttemptsExhaustedException(challengeId);
        }
        if (now.isAfter(expiresAt)) {
            expired = true;
            throw new OtpExpiredException(challengeId);
        }
        if (!matches(hasher.hash(challengeId, candidate))) {
            attempts++;
            if (attempts >= MAX_ATTEMPTS) {
                locked = true;
                throw new OtpAttemptsExhaustedException(challengeId);
            }
            throw new WrongOtpException(challengeId);
        }
    }

    private boolean matches(String candidateHash) {
        return MessageDigest.isEqual(
                codeHash.getBytes(StandardCharsets.UTF_8),
                candidateHash.getBytes(StandardCharsets.UTF_8));
    }

    /** The hashed code as stored at rest — never the plaintext (issue #77). */
    public String codeHash() {
        return codeHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public int attempts() {
        return attempts;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isExpired() {
        return expired;
    }
}
