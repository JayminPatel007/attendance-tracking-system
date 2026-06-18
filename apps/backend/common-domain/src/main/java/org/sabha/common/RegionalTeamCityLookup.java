package org.sabha.common;

import java.util.List;
import java.util.UUID;

/**
 * Cross-context read port (ADR-0019): the Cities a user is a Regional Team
 * member of. Drives Zone-creation authorization in the sabha context — Zone
 * creation moved from the Madhyastha Karyalaya down to the Regional Team
 * (ADR-0024, superseding the Zone row of ADR-0009) — and scopes the web's
 * Zone-create form, mirroring {@link SanyojakZoneLookup} for Kshetras.
 *
 * <p>Regional Team membership is recorded per {@code (City, demographic)}, but a
 * Zone is geography and therefore demographic-agnostic: <b>any</b> Regional Team
 * member of the target City may create a Zone there, regardless of which
 * demographic their role is scoped to. This port deliberately collapses the
 * demographic away and answers only at City granularity. The
 * {@code role_assignments} table is owned by identity, so the implementation
 * lives in identity-data-access; the port lives in common-domain so the sabha
 * context can consult it across the bounded-context seam.</p>
 */
public interface RegionalTeamCityLookup {

    /** The distinct City ids the user holds a Regional Team role on (empty if none). */
    List<UUID> citiesOf(UUID userId);

    /** Whether the user is a Regional Team member of the given City — derived from {@link #citiesOf}. */
    default boolean isRegionalTeamMemberOfCity(UUID userId, UUID cityId) {
        return citiesOf(userId).contains(cityId);
    }
}
