package org.sabha.identity.domain;

import org.sabha.common.DomainException;

/**
 * Raised when a Verified Home Sabha Transfer cannot find a Home Sabha of the
 * destination's demographic+track to swap (ADR-0002). Lateral transfers always
 * have one; this guards the case where there is nothing to replace.
 */
public class NoMatchingHomeSabhaException extends DomainException {

    public NoMatchingHomeSabhaException(String destinationKind) {
        super("Person has no Home Sabha of kind " + destinationKind + " to transfer");
    }
}
