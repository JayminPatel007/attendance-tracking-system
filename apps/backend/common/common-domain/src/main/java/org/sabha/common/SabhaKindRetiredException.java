package org.sabha.common;

import java.util.UUID;

/**
 * Thrown when a creation path — a new Sabha, a Sanchalak/Sah-Sanchalak role on a
 * Sabha, or a Home Sabha membership — references a Sabha Kind that has been
 * soft-retired (ADR-0026). Retirement blocks new usage while existing Sabhas,
 * roles, and Home Sabhas of that kind drain naturally, so this is a state
 * collision (HTTP 409) carrying a stable {@code code} the clients can branch on,
 * not a malformed request.
 */
public class SabhaKindRetiredException extends ConflictException {

    public SabhaKindRetiredException(UUID kindReference) {
        super("That Sabha Kind has been retired; no new Sabhas, roles, or Home Sabhas "
                + "of this kind can be created (" + kindReference + ")", "SABHA_KIND_RETIRED");
    }
}
