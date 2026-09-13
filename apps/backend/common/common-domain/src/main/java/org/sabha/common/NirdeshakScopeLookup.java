package org.sabha.common;

import java.util.List;
import java.util.UUID;

/**
 * Cross-context read port (ADR-0019): the {@code (Kshetra, demographic)} scopes a
 * user holds a Nirdeshak role on. The {@code role_assignments} table is owned by
 * identity, so the implementation lives in identity-data-access; the port lives in
 * common-domain so the sabha context can list a Nirdeshak's own Sabhas across the
 * bounded-context seam — scoping the web's Sabha-deletion list to exactly the
 * Sabhas the caller may delete (ADR-0026), mirroring {@link SanyojakZoneLookup}
 * and {@link RegionalTeamCityLookup} for the tiers above.
 */
public interface NirdeshakScopeLookup {

    /** The Nirdeshak ownership scopes the user holds (empty if none). */
    List<NirdeshakScope> scopesOf(UUID userId);

    /** A single Nirdeshak ownership scope: a Kshetra and the demographic within it. */
    record NirdeshakScope(UUID kshetraId, String demographic) {
    }
}
