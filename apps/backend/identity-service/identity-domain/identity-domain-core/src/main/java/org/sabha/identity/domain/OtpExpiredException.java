package org.sabha.identity.domain;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * The OTP for a Verified Home Sabha Transfer was entered after its 5-minute TTL
 * (ADR-0002 / PRD-0001). The transfer moves to {@code EXPIRED}; no swap occurs.
 */
public class OtpExpiredException extends DomainException {

    public OtpExpiredException(UUID transferId) {
        super("OTP for transfer " + transferId + " has expired");
    }
}
