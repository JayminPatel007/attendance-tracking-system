package org.sabha.identity.domain;

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
 */
public final class OtpChallenge {

    /** OTP time-to-live (PRD-0001 Implementation Decisions). */
    public static final Duration TTL = Duration.ofMinutes(5);

    /** Wrong-OTP attempts allowed before the challenge locks (PRD-0001). */
    public static final int MAX_ATTEMPTS = 5;

    private final UUID challengeId;
    private final String code;
    private final Instant expiresAt;
    private int attempts;
    private boolean locked;
    private boolean expired;

    private OtpChallenge(UUID challengeId, String code, Instant expiresAt,
                         int attempts, boolean locked, boolean expired) {
        this.challengeId = challengeId;
        this.code = code;
        this.expiresAt = expiresAt;
        this.attempts = attempts;
        this.locked = locked;
        this.expired = expired;
    }

    /** Issues a fresh challenge valid for {@link #TTL} from {@code now}. */
    public static OtpChallenge issue(UUID challengeId, String code, Instant now) {
        return new OtpChallenge(challengeId, code, now.plus(TTL), 0, false, false);
    }

    /** Rebuilds a persisted challenge from its stored columns, registering nothing. */
    public static OtpChallenge rehydrate(UUID challengeId, String code, Instant expiresAt,
                                         int attempts, boolean locked, boolean expired) {
        return new OtpChallenge(challengeId, code, expiresAt, attempts, locked, expired);
    }

    /**
     * Consumes a code attempt. Returns normally when {@code candidate} matches
     * within TTL; otherwise records the consequence (expiry / incremented attempt
     * / lockout) and throws the matching {@link org.sabha.common.DomainException}.
     */
    public void verify(String candidate, Instant now) {
        if (locked) {
            throw new OtpAttemptsExhaustedException(challengeId);
        }
        if (now.isAfter(expiresAt)) {
            expired = true;
            throw new OtpExpiredException(challengeId);
        }
        if (!code.equals(candidate)) {
            attempts++;
            if (attempts >= MAX_ATTEMPTS) {
                locked = true;
                throw new OtpAttemptsExhaustedException(challengeId);
            }
            throw new WrongOtpException(challengeId);
        }
    }

    public String code() {
        return code;
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
