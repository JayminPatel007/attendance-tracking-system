package org.sabha.identity.applicationservice.otp;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

/**
 * The production {@link OtpSendPolicy}: a rolling window with a per-window cap
 * plus a resend cooldown, both from PRD-0001. The numbers live here and nowhere
 * else.
 */
@Component
public class WindowedOtpSendPolicy implements OtpSendPolicy {

    /** Rolling window and cap for OTP sends per mobile (PRD-0001). */
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);
    private static final int MAX_OTPS_PER_WINDOW = 3;

    /** Resend cooldown between OTPs to the same mobile (PRD-0001). */
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);

    @Override
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
