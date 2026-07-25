package org.sabha.identity.applicationservice.otp;

import java.time.Instant;

/**
 * The OTP send budget (PRD-0001): the single place that owns the rolling window,
 * the per-window cap, and the resend cooldown for OTPs sent to a mobile.
 * {@link OtpGuardedFlow} asks it before every send, so both OTP flows — Verified
 * Home Sabha Transfer (ADR-0002) and self-service password reset (ADR-0004) —
 * get the same allow / cooldown / rate-limited decision.
 *
 * <p>Each flow still counts only its own sends: the policy owns the window and
 * asks the flow's {@link OtpSendLog} how many sends fall inside it. Whether the
 * budget should union across both flows is an open product question, deliberately
 * left unchanged here.
 *
 * <p>{@link WindowedOtpSendPolicy} holds the production rules; the seam is an
 * interface so a test can substitute the decision without subclassing them.
 */
public interface OtpSendPolicy {

    /**
     * Decides whether a new OTP may be sent to {@code mobile} right now, reading
     * the flow's prior sends from {@code log}.
     *
     * @throws OtpResendCooldownException    if the cooldown since the last send has not elapsed
     * @throws OtpRateLimitExceededException if the window's send cap has been reached
     */
    void enforce(String mobile, OtpSendLog log, Instant now);
}
