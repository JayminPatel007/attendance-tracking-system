package org.sabha.identity.domain;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * The OTP attempt budget (5, per PRD-0001) for an OTP-gated challenge was used up
 * — a Verified Home Sabha Transfer (ADR-0002) or a self-service password reset
 * (ADR-0004). The challenge is {@code LOCKED}; even the correct code is no longer
 * accepted and no state-changing action occurs.
 */
public class OtpAttemptsExhaustedException extends DomainException {

    public OtpAttemptsExhaustedException(UUID challengeId) {
        super("OTP attempts exhausted for challenge " + challengeId);
    }
}
