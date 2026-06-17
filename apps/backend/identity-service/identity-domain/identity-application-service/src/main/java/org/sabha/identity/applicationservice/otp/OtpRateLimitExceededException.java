package org.sabha.identity.applicationservice.otp;

/**
 * Raised when a mobile has already received the maximum OTPs in the rolling
 * window (3 per hour, PRD-0001). A transport-tier signal thrown by the
 * orchestrator — a sibling of the domain exceptions, mapped to HTTP 429.
 */
public class OtpRateLimitExceededException extends RuntimeException {

    public OtpRateLimitExceededException(String mobile) {
        super("OTP rate limit exceeded for mobile " + mobile);
    }
}
