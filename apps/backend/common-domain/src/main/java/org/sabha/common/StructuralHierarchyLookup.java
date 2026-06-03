package org.sabha.common;

import java.util.Optional;
import java.util.UUID;

/**
 * Cross-context read port (ADR-0019): walks the geographic containment chain
 * Sabha → Kshetra → Zone → City that owns the structural tables (sabha context).
 * The identity context's appointment Authorization Engine (ADR-0011) consults it
 * to resolve where a role being filled sits, then checks the appointer holds the
 * tier above at the parent scope. The {@code role_assignments} side of that check
 * is identity-owned and lives behind {@link AppointerAuthorityLookup} (no such
 * type in common-domain — it stays in identity).
 *
 * <p>The implementation reads only sabha-owned tables and lives in
 * {@code sabha-data-access}; the port lives in common-domain so identity can
 * consult it across the seam without depending on sabha's domain types.</p>
 */
public interface StructuralHierarchyLookup {

    /** The Kshetra and {@code (demographic, track)} kind of the given Sabha. */
    Optional<SabhaScope> sabhaScope(UUID sabhaId);

    /** The Zone the given Kshetra belongs to. */
    Optional<UUID> zoneOfKshetra(UUID kshetraId);

    /** The City the given Zone belongs to. */
    Optional<UUID> cityOfZone(UUID zoneId);
}
