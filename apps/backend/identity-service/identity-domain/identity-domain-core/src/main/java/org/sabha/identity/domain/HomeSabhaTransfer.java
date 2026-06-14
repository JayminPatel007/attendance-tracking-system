package org.sabha.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.AggregateRoot;

/**
 * The Verified Home Sabha Transfer aggregate (ADR-0002). Holds the OTP consent
 * state between {@code initiate} and {@code confirm} in a shared
 * {@link OtpChallenge} (the TTL, attempt budget, lockout and expiry live there);
 * the aggregate keeps only what is genuinely its own — whether the consent has
 * been {@code confirmed}, the Roster-swap hook, and its domain events. The Roster
 * swap and audit are orchestrated by the application service once {@link #confirm}
 * succeeds.
 *
 * <p>The lifecycle {@link TransferStatus} is <em>derived</em> from the challenge
 * and the {@code confirmed} flag, so there is a single source of truth for the OTP
 * sub-state ({@code EXPIRED} / {@code LOCKED}), mirroring {@link PasswordReset}.</p>
 */
public class HomeSabhaTransfer extends AggregateRoot<UUID> {

    private final UUID id;
    private final UUID personId;
    private final String mobile;
    private final UUID destinationSabhaId;
    private final UUID initiatingUserId;
    private final OtpChallenge challenge;
    private final Instant initiatedAt;
    private boolean confirmed;

    private HomeSabhaTransfer(UUID id, UUID personId, String mobile, UUID destinationSabhaId,
                              UUID initiatingUserId, OtpChallenge challenge, Instant initiatedAt) {
        this.id = id;
        this.personId = personId;
        this.mobile = mobile;
        this.destinationSabhaId = destinationSabhaId;
        this.initiatingUserId = initiatingUserId;
        this.challenge = challenge;
        this.initiatedAt = initiatedAt;
    }

    /**
     * Opens a transfer in {@code PENDING} with a freshly issued {@link OtpChallenge}
     * valid for {@link OtpChallenge#TTL} from {@code now}. Registers
     * {@link HomeSabhaTransferInitiated}.
     */
    public static HomeSabhaTransfer initiate(UUID id, UUID personId, String mobile,
                                             UUID destinationSabhaId, UUID initiatingUserId,
                                             String otpCode, Instant now) {
        HomeSabhaTransfer transfer = new HomeSabhaTransfer(
                id, personId, mobile, destinationSabhaId, initiatingUserId,
                OtpChallenge.issue(id, otpCode, now), now);
        transfer.registerEvent(new HomeSabhaTransferInitiated(
                id, personId, destinationSabhaId, initiatingUserId, now));
        return transfer;
    }

    /**
     * Rebuilds a persisted transfer from its stored columns, registering no events.
     * The persisted {@code status} is the single input for the lifecycle: this is
     * the exact inverse of {@link #status()}, kept here beside it so the two halves
     * of the mapping never drift, and the OTP sub-state is reconstructed into the
     * challenge (mirroring {@link PasswordReset#rehydrate}). The repository stays
     * unaware of how status decomposes into the challenge and the {@code confirmed}
     * flag.
     */
    public static HomeSabhaTransfer rehydrate(UUID id, UUID personId, String mobile,
                                              UUID destinationSabhaId, UUID initiatingUserId,
                                              String otpCode, Instant initiatedAt, Instant expiresAt,
                                              TransferStatus status, int attempts) {
        OtpChallenge challenge = OtpChallenge.rehydrate(
                id, otpCode, expiresAt, attempts,
                status == TransferStatus.LOCKED,
                status == TransferStatus.EXPIRED);
        HomeSabhaTransfer transfer = new HomeSabhaTransfer(
                id, personId, mobile, destinationSabhaId, initiatingUserId, challenge, initiatedAt);
        transfer.confirmed = status == TransferStatus.CONFIRMED;
        return transfer;
    }

    /** Records that the consent OTP was dispatched; registers {@link TransferOtpSent}. */
    public void markOtpSent(Instant now) {
        registerEvent(new TransferOtpSent(id, now));
    }

    /**
     * Consumes the Person's OTP through the shared {@link OtpChallenge}. On a correct
     * code within TTL the transfer becomes {@code CONFIRMED} and registers
     * {@link TransferOtpConfirmed}. Wrong / expired / exhausted entries throw the
     * typed OTP exception (carrying this transfer's id) and leave it unconfirmed.
     */
    public void confirm(String code, Instant now) {
        challenge.verify(code, now);
        confirmed = true;
        registerEvent(new TransferOtpConfirmed(id, personId, now));
    }

    /**
     * Records that the Roster swap committed, moving the Person off
     * {@code previousSabhaId} onto the destination; registers {@link HomeSabhaSwapped}.
     */
    public void recordSwap(UUID previousSabhaId, Instant now) {
        registerEvent(new HomeSabhaSwapped(id, personId, previousSabhaId, destinationSabhaId, now));
    }

    @Override
    public UUID id() {
        return id;
    }

    public UUID personId() {
        return personId;
    }

    public String mobile() {
        return mobile;
    }

    public UUID destinationSabhaId() {
        return destinationSabhaId;
    }

    public UUID initiatingUserId() {
        return initiatingUserId;
    }

    public Instant initiatedAt() {
        return initiatedAt;
    }

    public OtpChallenge challenge() {
        return challenge;
    }

    /**
     * The lifecycle derived from the {@code confirmed} flag and the OTP sub-state.
     * The exact inverse of the {@code status} input to {@link #rehydrate}.
     */
    public TransferStatus status() {
        if (confirmed) {
            return TransferStatus.CONFIRMED;
        }
        if (challenge.isLocked()) {
            return TransferStatus.LOCKED;
        }
        if (challenge.isExpired()) {
            return TransferStatus.EXPIRED;
        }
        return TransferStatus.PENDING;
    }
}
