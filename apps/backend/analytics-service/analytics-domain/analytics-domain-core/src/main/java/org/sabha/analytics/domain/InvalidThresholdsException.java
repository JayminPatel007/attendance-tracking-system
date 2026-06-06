package org.sabha.analytics.domain;

import org.sabha.common.DomainException;

/** The re-engagement thresholds violate their invariant ({@code priority >= candidate >= 1}). */
public class InvalidThresholdsException extends DomainException {

    public InvalidThresholdsException(String message) {
        super(message);
    }
}
