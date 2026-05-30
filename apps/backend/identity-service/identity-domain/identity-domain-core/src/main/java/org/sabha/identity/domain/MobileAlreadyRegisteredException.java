package org.sabha.identity.domain;

import org.sabha.common.DomainException;

/**
 * The mobile hard block (ADR-0013): an exact match on an existing Person's
 * own-mobile stops the add and surfaces that Person so the adder selects them or
 * cancels. Carries the existing Person so the presentation layer can show their
 * profile. Mapped to HTTP 409.
 */
public class MobileAlreadyRegisteredException extends DomainException {

    private final transient Person existing;

    public MobileAlreadyRegisteredException(Person existing) {
        super("Mobile already registered to Person " + existing.id());
        this.existing = existing;
    }

    public Person existing() {
        return existing;
    }
}
