package org.sabha.identity.domain;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * The OTP entered for an OTP-gated challenge did not match — a Verified Home
 * Sabha Transfer (ADR-0002) or a self-service password reset (ADR-0004). The
 * failed attempt counts against the challenge's budget; no state-changing action
 * is taken.
 */
public class WrongOtpException extends DomainException {

    public WrongOtpException(UUID challengeId) {
        super("Incorrect OTP for challenge " + challengeId);
    }
}
