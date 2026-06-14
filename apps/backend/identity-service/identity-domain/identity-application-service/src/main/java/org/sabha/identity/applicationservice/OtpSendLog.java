package org.sabha.identity.applicationservice;

import java.time.Instant;
import java.util.Optional;

/**
 * The per-mobile record of OTP sends a flow keeps, as {@link OtpSendPolicy} reads
 * it to apply the send budget (PRD-0001). Each OTP-sending flow's repository is
 * such a log; the policy owns the window and asks the log how many sends fall
 * inside it, so each flow keeps counting against only its own sends.
 */
public interface OtpSendLog {

    /** When the most recent OTP was sent to {@code mobile}, if any. */
    Optional<Instant> lastInitiatedAt(String mobile);

    /** How many OTPs were sent to {@code mobile} at or after {@code since}. */
    int sendCountSince(String mobile, Instant since);
}
