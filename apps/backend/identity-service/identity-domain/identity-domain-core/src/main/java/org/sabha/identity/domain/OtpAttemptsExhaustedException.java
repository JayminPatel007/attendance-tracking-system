package org.sabha.identity.domain;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * The OTP attempt budget (5, per PRD-0001) for a Verified Home Sabha Transfer was
 * used up (ADR-0002). The transfer is {@code LOCKED}; even the correct code is no
 * longer accepted and no swap occurs.
 */
public class OtpAttemptsExhaustedException extends DomainException {

    public OtpAttemptsExhaustedException(UUID transferId) {
        super("OTP attempts exhausted for transfer " + transferId);
    }
}
