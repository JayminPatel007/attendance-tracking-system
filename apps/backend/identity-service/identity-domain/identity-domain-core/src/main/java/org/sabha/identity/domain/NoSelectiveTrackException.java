package org.sabha.identity.domain;

import org.sabha.common.DomainException;

/**
 * A nomination was attempted from a demographic that has no selective track
 * (ADR-0006) — only Baal/Balika (→ BSS) and Yuvak/Yuvati (→ YSS) participate in a
 * selective program; the all-encompassing Sanyukta kind does not.
 */
public class NoSelectiveTrackException extends DomainException {

    public NoSelectiveTrackException(String demographic) {
        super("Demographic " + demographic + " has no selective (BSS/YSS) track");
    }
}
