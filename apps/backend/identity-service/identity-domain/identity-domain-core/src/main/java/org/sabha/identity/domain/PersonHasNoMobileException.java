package org.sabha.identity.domain;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * A Verified Home Sabha Transfer needs an OTP to the Person's own mobile, but the
 * Person is guardian-linked with no mobile of their own (ADR-0002 / ADR-0013).
 * Such cases fall back to the top-down (Nirdeshak) path, which is out of scope
 * for this slice.
 */
public class PersonHasNoMobileException extends DomainException {

    public PersonHasNoMobileException(UUID personId) {
        super("Person " + personId + " has no mobile to receive a transfer OTP");
    }
}
