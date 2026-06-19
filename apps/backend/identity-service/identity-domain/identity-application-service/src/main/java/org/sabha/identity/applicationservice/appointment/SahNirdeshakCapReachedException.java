package org.sabha.identity.applicationservice.appointment;

import org.sabha.common.ConflictException;

/**
 * Raised when appointing a Sah-Nirdeshak would exceed the cap of two active
 * Sah-Nirdeshaks per (Kshetra, demographic) (ADR-0025 §3). Surfaced as HTTP 409
 * with a stable {@code code} so the web role console can disable the appoint
 * action at the limit; nothing is committed.
 */
public class SahNirdeshakCapReachedException extends ConflictException {

    public SahNirdeshakCapReachedException(int cap) {
        super("A Kshetra already has the maximum of " + cap
                + " Sah-Nirdeshaks for this demographic. Revoke one before appointing another.",
                "SAH_NIRDESHAK_CAP_REACHED");
    }
}
