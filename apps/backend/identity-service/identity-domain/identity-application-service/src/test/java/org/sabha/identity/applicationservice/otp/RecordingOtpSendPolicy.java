package org.sabha.identity.applicationservice.otp;

import java.time.Instant;

/**
 * Captures what the flow hands the send budget, and optionally vetoes the send.
 * Implements the {@link OtpSendPolicy} port rather than subclassing the live
 * {@link WindowedOtpSendPolicy} rules, so a change to the window or the cap can
 * never silently change what this fixture does.
 */
final class RecordingOtpSendPolicy implements OtpSendPolicy {

    String mobile;
    OtpSendLog log;
    Instant now;
    RuntimeException toThrow;

    @Override
    public void enforce(String mobile, OtpSendLog log, Instant now) {
        this.mobile = mobile;
        this.log = log;
        this.now = now;
        if (toThrow != null) {
            throw toThrow;
        }
    }
}
