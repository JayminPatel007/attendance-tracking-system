package org.sabha.identity.domain;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * The OTP for an OTP-gated challenge was entered after its 5-minute TTL (ADR-0002
 * / ADR-0004 / PRD-0001). The challenge moves to its {@code EXPIRED} state; no
 * state-changing action occurs.
 */
public class OtpExpiredException extends DomainException {

    public OtpExpiredException(UUID challengeId) {
        super("OTP for challenge " + challengeId + " has expired");
    }
}
