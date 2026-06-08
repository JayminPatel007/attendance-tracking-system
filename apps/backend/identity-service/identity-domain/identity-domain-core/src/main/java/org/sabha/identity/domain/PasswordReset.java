package org.sabha.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.sabha.common.AggregateRoot;

/**
 * The self-service password-reset aggregate (ADR-0004). Holds the OTP consent
 * state between {@code request} and the later verify/complete steps via a shared
 * {@link OtpChallenge}; once the OTP is verified it issues a short-lived reset
 * token the client echoes to set the new password. The actual credential change
 * is delegated to the identity provider by the application service.
 *
 * <p>The lifecycle {@link PasswordResetStatus} is <em>derived</em> from the
 * challenge and the verify/complete flags, so there is a single source of truth
 * for the OTP sub-state ({@code EXPIRED} / {@code LOCKED}).</p>
 */
public class PasswordReset extends AggregateRoot<UUID> {

    /** Window to set a new password after the OTP is verified. */
    public static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(5);

    private final UUID id;
    private final UUID userId;
    private final UUID keycloakUserId;
    private final String mobile;
    private final OtpChallenge challenge;
    private final Instant initiatedAt;
    private boolean verified;
    private boolean completed;
    private String resetToken;
    private Instant resetTokenExpiresAt;

    private PasswordReset(UUID id, UUID userId, UUID keycloakUserId, String mobile,
                          OtpChallenge challenge, Instant initiatedAt) {
        this.id = id;
        this.userId = userId;
        this.keycloakUserId = keycloakUserId;
        this.mobile = mobile;
        this.challenge = challenge;
        this.initiatedAt = initiatedAt;
    }

    /**
     * Opens a reset in {@code PENDING} with a freshly issued {@link OtpChallenge}.
     * Registers {@link PasswordResetRequested}.
     */
    public static PasswordReset request(UUID id, UUID userId, UUID keycloakUserId, String mobile,
                                        String otpCode, Instant now) {
        PasswordReset reset = new PasswordReset(
                id, userId, keycloakUserId, mobile, OtpChallenge.issue(id, otpCode, now), now);
        reset.registerEvent(new PasswordResetRequested(id, userId, now));
        return reset;
    }

    /**
     * Rebuilds a persisted reset from its stored columns, registering no events
     * (mirrors {@link HomeSabhaTransfer#rehydrate}). The persisted {@code status}
     * is the single input for the lifecycle: this is the exact inverse of
     * {@link #status()}, kept here beside it so the two halves of the mapping never
     * drift. The repository stays unaware of how status decomposes into the OTP
     * sub-state and the verify/complete flags.
     */
    public static PasswordReset rehydrate(UUID id, UUID userId, UUID keycloakUserId, String mobile,
                                          String otpCode, Instant otpExpiresAt, int attempts,
                                          PasswordResetStatus status, Instant initiatedAt,
                                          String resetToken, Instant resetTokenExpiresAt) {
        OtpChallenge challenge = OtpChallenge.rehydrate(
                id, otpCode, otpExpiresAt, attempts,
                status == PasswordResetStatus.LOCKED,
                status == PasswordResetStatus.EXPIRED);
        PasswordReset reset = new PasswordReset(id, userId, keycloakUserId, mobile, challenge, initiatedAt);
        reset.verified = status == PasswordResetStatus.VERIFIED || status == PasswordResetStatus.COMPLETED;
        reset.completed = status == PasswordResetStatus.COMPLETED;
        reset.resetToken = resetToken;
        reset.resetTokenExpiresAt = resetTokenExpiresAt;
        return reset;
    }

    /** Records that the reset OTP was dispatched; registers {@link PasswordResetOtpSent}. */
    public void markOtpSent(Instant now) {
        registerEvent(new PasswordResetOtpSent(id, now));
    }

    /**
     * Consumes the reset OTP through the {@link OtpChallenge}. On a correct code
     * within TTL the reset becomes {@code VERIFIED}, stores {@code resetToken}
     * (valid for {@link #RESET_TOKEN_TTL}), and registers {@link PasswordResetVerified}.
     * Wrong / expired / exhausted entries throw and leave no token.
     */
    public void verify(String code, String resetToken, Instant now) {
        challenge.verify(code, now);
        this.resetToken = resetToken;
        this.resetTokenExpiresAt = now.plus(RESET_TOKEN_TTL);
        this.verified = true;
        registerEvent(new PasswordResetVerified(id, now));
    }

    /**
     * Marks the reset complete once the new password has been accepted by the
     * identity provider; registers {@link PasswordResetCompleted}.
     */
    public void complete(Instant now) {
        if (completed) {
            throw new ResetAlreadyCompletedException(id);
        }
        if (now.isAfter(resetTokenExpiresAt)) {
            throw new ResetTokenExpiredException(id);
        }
        this.completed = true;
        registerEvent(new PasswordResetCompleted(id, userId, now));
    }

    @Override
    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public UUID keycloakUserId() {
        return keycloakUserId;
    }

    public String resetToken() {
        return resetToken;
    }

    public Instant resetTokenExpiresAt() {
        return resetTokenExpiresAt;
    }

    public String mobile() {
        return mobile;
    }

    public OtpChallenge challenge() {
        return challenge;
    }

    public Instant initiatedAt() {
        return initiatedAt;
    }

    public PasswordResetStatus status() {
        if (completed) {
            return PasswordResetStatus.COMPLETED;
        }
        if (verified) {
            return PasswordResetStatus.VERIFIED;
        }
        if (challenge.isLocked()) {
            return PasswordResetStatus.LOCKED;
        }
        if (challenge.isExpired()) {
            return PasswordResetStatus.EXPIRED;
        }
        return PasswordResetStatus.PENDING;
    }
}
