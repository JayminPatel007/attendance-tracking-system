package org.sabha.identity.applicationservice;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

/**
 * The OTP send budget (PRD-0001): the single place that owns the rolling window,
 * the per-window cap, and the resend cooldown for OTPs sent to a mobile. Both OTP
 * flows — Verified Home Sabha Transfer (ADR-0002) and self-service password reset
 * (ADR-0004) — delegate the allow / cooldown / rate-limited decision here.
 *
 * <p>Each flow still counts only its own sends: the policy owns the window and
 * asks the flow's {@link OtpSendLog} how many sends fall inside it. Whether the
 * budget should union across both flows is an open product question, deliberately
 * left unchanged here.
 */
@Component
public class OtpSendPolicy {

    /** Rolling window and cap for OTP sends per mobile (PRD-0001). */
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);
    private static final int MAX_OTPS_PER_WINDOW = 3;

    /** Resend cooldown between OTPs to the same mobile (PRD-0001). */
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);

    /**
     * Decides whether a new OTP may be sent to {@code mobile} right now, reading
     * the flow's prior sends from {@code log}.
     *
     * @throws OtpResendCooldownException    if the cooldown since the last send has not elapsed
     * @throws OtpRateLimitExceededException if the window's send cap has been reached
     */
    public void enforce(String mobile, OtpSendLog log, Instant now) {
        log.lastInitiatedAt(mobile).ifPresent(last -> {
            if (Duration.between(last, now).compareTo(RESEND_COOLDOWN) < 0) {
                throw new OtpResendCooldownException(mobile);
            }
        });
        if (log.sendCountSince(mobile, now.minus(RATE_LIMIT_WINDOW)) >= MAX_OTPS_PER_WINDOW) {
            throw new OtpRateLimitExceededException(mobile);
        }
    }
}
