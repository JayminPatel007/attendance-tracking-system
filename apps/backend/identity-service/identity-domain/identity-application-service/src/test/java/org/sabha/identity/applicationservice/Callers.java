package org.sabha.identity.applicationservice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.sabha.common.CallerAuthority;
import org.sabha.common.RoleAssignment;
import org.sabha.common.UserId;

/**
 * Builds the {@link CallerAuthority} literals identity's tests authorize
 * against. Public so the per-feature test packages can share one spelling of
 * "a caller holding this row".
 *
 * <p>Before ADR-0032 each of these sentences needed a test double —
 * {@code AppointerAuthorityLookup}, {@code MadhyasthaKaryalayaLookup},
 * {@code RoleAssignmentLookup}, {@code UserRolesLookup} — and the doubles were
 * keyed by concatenated strings because the real questions were keyed by
 * {@code (user, scope, demographic)}. The rows those four fakes were pretending
 * to hold are now the thing under test, so they are written out directly.</p>
 *
 * <p>The {@code role} values are the wire strings stored in {@code
 * role_assignments.role}, and each tier fills exactly the scope column it means.
 * That is deliberately not hidden behind an enum here: four overlapping enums
 * partition that column three different ways (issue #232), and a test that
 * asserts on the stored shape should say the stored shape.</p>
 */
public final class Callers {

    private final UUID userId;
    private final List<RoleAssignment> rows = new ArrayList<>();

    private Callers(UUID userId) {
        this.userId = userId;
    }

    public static Callers of(UUID userId) {
        return new Callers(userId);
    }

    /** A caller holding no role at all — every authority question answers false. */
    public static CallerAuthority noRoles(UUID userId) {
        return CallerAuthority.withNoRoles(UserId.of(userId));
    }

    /** State-level oversight: a null-scope row (ADR-0005). */
    public static CallerAuthority madhyasthaKaryalaya(UUID userId) {
        return of(userId).mk().build();
    }

    public Callers mk() {
        return row("MADHYASTHA_KARYALAYA", null, null, null, null, null);
    }

    /** A Sant — universal read, no appointer (ADR-0011), outside the operational Role enum. */
    public Callers sant() {
        return row("SANT", null, null, null, null, null);
    }

    /** The appointing tier over a {@code (Kshetra, demographic)} (ADR-0011). */
    public Callers nirdeshakOf(UUID kshetraId, String demographic) {
        return row("NIRDESHAK", null, kshetraId, null, null, demographic);
    }

    public Callers sahNirdeshakOf(UUID kshetraId, String demographic) {
        return row("SAH_NIRDESHAK", null, kshetraId, null, null, demographic);
    }

    public Callers sanyojakOf(UUID zoneId, String demographic) {
        return row("SANYOJAK", null, null, zoneId, null, demographic);
    }

    public Callers regionalTeamIn(UUID cityId, String demographic) {
        return row("REGIONAL_TEAM", null, null, null, cityId, demographic);
    }

    /** Runs a Sabha — the Sanchalak half of {@code CallerAuthority.runsSabha}. */
    public Callers sanchalakOf(UUID sabhaId) {
        return row("SANCHALAK", sabhaId, null, null, null, null);
    }

    /** Runs a Sabha — the Sah-Sanchalak half, which shares nomination and transfer authority. */
    public Callers sahSanchalakOf(UUID sabhaId) {
        return row("SAH_SANCHALAK", sabhaId, null, null, null, null);
    }

    public Callers nirikshakOf(UUID sabhaId) {
        return row("NIRIKSHAK", sabhaId, null, null, null, null);
    }

    public CallerAuthority build() {
        return new CallerAuthority(UserId.of(userId), rows);
    }

    private Callers row(String role, UUID sabhaId, UUID kshetraId,
                        UUID zoneId, UUID cityId, String demographic) {
        rows.add(new RoleAssignment(role, sabhaId, kshetraId, zoneId, cityId, demographic));
        return this;
    }
}
