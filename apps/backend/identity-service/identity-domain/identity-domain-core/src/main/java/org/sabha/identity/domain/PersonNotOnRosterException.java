package org.sabha.identity.domain;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * A selective nomination must come from the Regular Sabha's own Roster (ADR-0006);
 * raised when the nominated Person does not have the nominating Sabha among their
 * Home Sabhas.
 */
public class PersonNotOnRosterException extends DomainException {

    public PersonNotOnRosterException(UUID personId, UUID regularSabhaId) {
        super("Person " + personId + " is not on the Roster of Sabha " + regularSabhaId);
    }
}
