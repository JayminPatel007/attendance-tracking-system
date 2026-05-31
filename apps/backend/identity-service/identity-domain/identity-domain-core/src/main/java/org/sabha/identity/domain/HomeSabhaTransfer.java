package org.sabha.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.sabha.common.AggregateRoot;

/**
 * The Verified Home Sabha Transfer aggregate (ADR-0002). Holds the OTP consent
 * state between {@code initiate} and {@code confirm}: the code, its expiry, the
 * attempt budget, and the lifecycle {@link TransferStatus}. The Roster swap and
 * audit are orchestrated by the application service once {@link #confirm} succeeds.
 */
public class HomeSabhaTransfer extends AggregateRoot<UUID> {

    /** OTP time-to-live (PRD-0001 Implementation Decisions). */
    public static final Duration OTP_TTL = Duration.ofMinutes(5);

    /** Wrong-OTP attempts allowed before the transfer locks (PRD-0001). */
    public static final int MAX_ATTEMPTS = 5;

    private final UUID id;
    private final UUID personId;
    private final String mobile;
    private final UUID destinationSabhaId;
    private final UUID initiatingUserId;
    private final String otpCode;
    private final Instant initiatedAt;
    private final Instant expiresAt;
    private TransferStatus status;
    private int attempts;

    private HomeSabhaTransfer(UUID id, UUID personId, String mobile, UUID destinationSabhaId,
                              UUID initiatingUserId, String otpCode, Instant initiatedAt,
                              Instant expiresAt, TransferStatus status) {
        this.id = id;
        this.personId = personId;
        this.mobile = mobile;
        this.destinationSabhaId = destinationSabhaId;
        this.initiatingUserId = initiatingUserId;
        this.otpCode = otpCode;
        this.initiatedAt = initiatedAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    /**
     * Opens a transfer in {@code PENDING} with a freshly generated OTP valid for
     * {@link #OTP_TTL} from {@code now}. Registers {@link HomeSabhaTransferInitiated}.
     */
    public static HomeSabhaTransfer initiate(UUID id, UUID personId, String mobile,
                                             UUID destinationSabhaId, UUID initiatingUserId,
                                             String otpCode, Instant now) {
        HomeSabhaTransfer transfer = new HomeSabhaTransfer(
                id, personId, mobile, destinationSabhaId, initiatingUserId, otpCode,
                now, now.plus(OTP_TTL), TransferStatus.PENDING);
        transfer.registerEvent(new HomeSabhaTransferInitiated(
                id, personId, destinationSabhaId, initiatingUserId, now));
        return transfer;
    }

    /**
     * Rehydrates a persisted transfer without registering any domain events
     * (mirrors the data-access rehydration constructor on {@code Occurrence}).
     */
    public static HomeSabhaTransfer rehydrate(UUID id, UUID personId, String mobile,
                                              UUID destinationSabhaId, UUID initiatingUserId,
                                              String otpCode, Instant initiatedAt, Instant expiresAt,
                                              TransferStatus status, int attempts) {
        HomeSabhaTransfer transfer = new HomeSabhaTransfer(
                id, personId, mobile, destinationSabhaId, initiatingUserId, otpCode,
                initiatedAt, expiresAt, status);
        transfer.attempts = attempts;
        return transfer;
    }

    /** Records that the consent OTP was dispatched; registers {@link TransferOtpSent}. */
    public void markOtpSent(Instant now) {
        registerEvent(new TransferOtpSent(id, now));
    }

    /**
     * Consumes the Person's OTP. On a correct code within TTL the transfer becomes
     * {@code CONFIRMED} and registers {@link TransferOtpConfirmed}.
     */
    public void confirm(String code, Instant now) {
        if (status == TransferStatus.LOCKED) {
            throw new OtpAttemptsExhaustedException(id);
        }
        if (now.isAfter(expiresAt)) {
            status = TransferStatus.EXPIRED;
            throw new OtpExpiredException(id);
        }
        if (!otpCode.equals(code)) {
            attempts++;
            if (attempts >= MAX_ATTEMPTS) {
                status = TransferStatus.LOCKED;
                throw new OtpAttemptsExhaustedException(id);
            }
            throw new WrongOtpException(id);
        }
        status = TransferStatus.CONFIRMED;
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

    public Instant expiresAt() {
        return expiresAt;
    }

    public String otpCode() {
        return otpCode;
    }

    public int attempts() {
        return attempts;
    }

    public TransferStatus status() {
        return status;
    }
}
