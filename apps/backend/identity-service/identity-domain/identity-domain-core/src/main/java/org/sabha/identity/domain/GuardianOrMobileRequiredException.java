package org.sabha.identity.domain;

import org.sabha.common.DomainException;

/**
 * Thrown when a Person is created with neither their own mobile nor a guardian
 * link. Per ADR-0013 mobile is required at creation, with the single exception
 * that a child without their own phone is recorded against a guardian's Person
 * record. Mapped to HTTP 422.
 */
public class GuardianOrMobileRequiredException extends DomainException {

    public GuardianOrMobileRequiredException() {
        super("A Person must have either their own mobile or a guardian link");
    }
}
