package org.sabha.identity.domain;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * Deselection only applies to a currently {@code APPROVED} selection (ADR-0006):
 * raised when a deselect is attempted on a nomination in any other state.
 */
public class NotApprovedException extends DomainException {

    public NotApprovedException(UUID nominationId, NominationStatus status) {
        super("Nomination " + nominationId + " is " + status + ", not APPROVED, so it cannot be deselected");
    }
}
