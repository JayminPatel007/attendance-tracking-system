package org.sabha.identity.domain;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * A nomination can only be decided once (ADR-0006): raised when an approve or
 * reject is attempted on a nomination that is no longer {@code PENDING}, so the
 * audit trail's decider and timestamp are never overwritten.
 */
public class NominationAlreadyDecidedException extends DomainException {

    public NominationAlreadyDecidedException(UUID nominationId, NominationStatus status) {
        super("Nomination " + nominationId + " is already " + status + " and cannot be decided again");
    }
}
