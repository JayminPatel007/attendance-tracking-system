package org.sabha.identity.applicationservice;

/**
 * Raised when a new OTP is requested for a mobile before the 30-second resend
 * cooldown has elapsed (PRD-0001). A transport-tier signal thrown by the
 * orchestrator, mapped to HTTP 429.
 */
public class OtpResendCooldownException extends RuntimeException {

    public OtpResendCooldownException(String mobile) {
        super("OTP resend cooldown not elapsed for mobile " + mobile);
    }
}
