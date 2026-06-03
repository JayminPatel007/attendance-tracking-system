package org.sabha.common;

import java.util.List;
import java.util.UUID;

/**
 * Cross-context read port (ADR-0019): the Zones a user is a Sanyojak of. Drives
 * structural-creation authorization for Kshetra creation — a Sanyojak creates
 * Kshetras within their Zone (ADR-0009) — and scopes the web's Kshetra-create
 * form. The {@code role_assignments} table is owned by identity, so the
 * implementation lives in identity-data-access; the port lives in common-domain
 * so the sabha context can consult it across the bounded-context seam.
 */
public interface SanyojakZoneLookup {

    /** The Zone ids the user holds a Sanyojak role on (empty if none). */
    List<UUID> zonesOf(UUID userId);

    /** Whether the user is a Sanyojak of the given Zone — derived from {@link #zonesOf}. */
    default boolean isSanyojakOfZone(UUID userId, UUID zoneId) {
        return zonesOf(userId).contains(zoneId);
    }
}
