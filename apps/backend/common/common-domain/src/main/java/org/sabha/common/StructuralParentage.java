package org.sabha.common;

import java.util.Optional;
import java.util.UUID;

/**
 * Cross-context read port (ADR-0019, ADR-0033): the geographic containment chain
 * above a Kshetra — Kshetra → Zone → City — over the sabha-owned tables.
 *
 * <p>The identity context's appointment Authorization Engine (ADR-0011) consults
 * it to resolve where a role being filled sits, then checks the appointer holds
 * the tier above at the parent scope. The {@code role_assignments} side of that
 * check is identity-owned and stays in identity (ADR-0029).</p>
 *
 * <p><strong>Kept separate from {@link SabhaFacts} on purpose.</strong> The chain
 * is never walked: {@code AppointmentAuthorization} takes one hop per branch of
 * one switch, so folding the hops into a join would save zero round trips
 * (ADR-0033, fact 4). A caller that ever needs two or more hops at once is the
 * recorded trigger to add a folded {@code parentageOf} here.</p>
 *
 * <p>The implementation reads only sabha-owned tables and lives in
 * {@code sabha-data-access}.</p>
 */
public interface StructuralParentage {

    /** The Zone the given Kshetra belongs to. */
    Optional<UUID> zoneOfKshetra(UUID kshetraId);

    /** The City the given Zone belongs to. */
    Optional<UUID> cityOfZone(UUID zoneId);
}
