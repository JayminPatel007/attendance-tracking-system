package org.sabha.sabha.domain;

import org.sabha.common.DomainException;

/**
 * Thrown when a Sanyukta {@link SabhaKind} is registered on a selective track
 * (BSS or YSS). The all-encompassing Sanyukta kind exists on the Regular track
 * only — the selective concept does not apply to it (CONTEXT § Sanyukta Sabha,
 * ADR-0009). Mapped to HTTP 422.
 */
public class SanyuktaMustBeRegularTrackException extends DomainException {

    public SanyuktaMustBeRegularTrackException(Track track) {
        super("Sanyukta is Regular-track only; cannot register it on " + track);
    }
}
