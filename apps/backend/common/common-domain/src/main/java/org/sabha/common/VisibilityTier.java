package org.sabha.common;

/**
 * The tiers of "which Sabhas can this caller see" that {@link CallerVisibility}
 * can grant, each a distinct branch of the role-scoped visibility predicate. Most
 * map to a {@code role_assignments} row matched at the tier's geographic level
 * (Sabha, Kshetra × demographic, Zone × demographic, City × demographic, or
 * unrestricted for the Madhyastha Karyalaya).
 *
 * <p>This is intentionally <em>not</em> {@link Role}: it includes the oversight
 * tiers {@link #REGIONAL_TEAM} and {@link #MADHYASTHA_KARYALAYA}, which {@code Role}
 * deliberately omits, and it splits the Nirikshak into two genuinely different
 * visibility rules:</p>
 * <ul>
 *   <li>{@link #NIRIKSHAK} — the Kshetra-tier reopen authority, resolved through a
 *       {@code role_assignments} row on {@code (kshetra, demographic)} exactly like a
 *       Nirdeshak (ADR-0001, ADR-0011); this is what the reopen read model grants.</li>
 *   <li>{@link #NIRIKSHAK_PROXY} — the explicit, mutable set of Sabhas a Nirikshak
 *       proxies, resolved through {@code nirikshak_sabha_assignments} (Slice 14); this
 *       is what the dashboard read model grants.</li>
 * </ul>
 *
 * <p><strong>Invariant:</strong> each role-assignment constant's name is identical to
 * its {@code role_assignments.role} value and to the matching {@link Role} constant.
 * {@link CallerVisibility} relies on this in both directions — it maps a {@link Role}
 * to a tier via {@link CallerVisibility#tiersFor} and emits the {@code role_assignments.role}
 * literal straight from the tier name — so renaming a constant here must move in lockstep
 * with {@code Role} and the database. ({@link #NIRIKSHAK_PROXY} is the exception: it is not
 * a {@code role_assignments.role} value and has no {@link Role}; it names the proxy table.)</p>
 */
public enum VisibilityTier {
    SANCHALAK,
    SAH_SANCHALAK,
    NIRIKSHAK,
    NIRDESHAK,
    SAH_NIRDESHAK,
    SANYOJAK,
    REGIONAL_TEAM,
    MADHYASTHA_KARYALAYA,
    NIRIKSHAK_PROXY
}
