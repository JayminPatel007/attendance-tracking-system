package org.sabha.identity.domain;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * The OTP entered for a Verified Home Sabha Transfer did not match (ADR-0002).
 * The failed attempt counts against the per-transfer budget; the Home Sabha is
 * unchanged.
 */
public class WrongOtpException extends DomainException {

    public WrongOtpException(UUID transferId) {
        super("Incorrect OTP for transfer " + transferId);
    }
}
