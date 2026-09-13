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

    /**
     * The Sabha of the given {@code (demographic, track)} kind within the Kshetra,
     * if one exists. Used by the BSS/YSS selection workflow (ADR-0006) to resolve
     * the selective Sabha a nominee would additionally join from their Regular
     * Sabha's Kshetra and demographic.
     *
     * <p>Defaulted to empty so the appointment/attendance test fakes that predate
     * the selection workflow need not implement it; the JDBC adapter overrides.</p>
     */
    default Optional<UUID> selectiveSabhaIn(UUID kshetraId, String demographic, String track) {
        return Optional.empty();
    }

    /**
     * Whether the given Sabha's {@code (demographic, track)} kind has been
     * soft-retired (ADR-0026). Identity's creation paths — appointing a
     * Sanchalak/Sah-Sanchalak, assigning or transferring a Home Sabha — consult
     * this to reject new usage of a retired kind while existing Sabhas drain.
     *
     * <p>Defaulted to {@code false} so the appointment/transfer test fakes that
     * predate soft-retire need not implement it; the JDBC adapter overrides.</p>
     */
    default boolean isSabhaKindRetired(UUID sabhaId) {
        return false;
    }

    /** The Zone the given Kshetra belongs to. */
    Optional<UUID> zoneOfKshetra(UUID kshetraId);

    /** The City the given Zone belongs to. */
    Optional<UUID> cityOfZone(UUID zoneId);
}
