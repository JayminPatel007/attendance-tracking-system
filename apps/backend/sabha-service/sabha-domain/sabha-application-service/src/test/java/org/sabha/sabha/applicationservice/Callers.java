package org.sabha.sabha.applicationservice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.sabha.common.CallerAuthority;
import org.sabha.common.RoleAssignment;
import org.sabha.common.UserId;

/**
 * Builds the {@link CallerAuthority} literals this module's tests authorize
 * against. It replaces {@code FakeRoleAssignments}, and the replacement is the
 * point: the structural services used to authorize through a {@code
 * StructuralScopeAuthority} engine over four ports, so a test had to stand up
 * four doubles to say "this caller is a Sanyojak of that Zone". ADR-0032 deleted
 * the engine — it held no policy of its own — and the same sentence is now one
 * row.
 *
 * <p>Rows are spelled the way the single adapter reads them: the {@code role}
 * string is the wire value, and each tier populates exactly the one scope column
 * it means. Getting that wrong is a real failure the tests should be able to
 * express, which is why this builds rows rather than stubbing answers.</p>
 */
final class Callers {

    private final UUID userId;
    private final List<RoleAssignment> rows = new ArrayList<>();

    private Callers(UUID userId) {
        this.userId = userId;
    }

    static Callers of(UUID userId) {
        return new Callers(userId);
    }

    /** A caller holding no role at all — every authority question answers false. */
    static CallerAuthority noRoles(UUID userId) {
        return CallerAuthority.withNoRoles(UserId.of(userId));
    }

    /** State scope (ADR-0005): a null-scope row, which is how MK is stored. */
    static CallerAuthority madhyasthaKaryalaya(UUID userId) {
        return of(userId).mk().build();
    }

    Callers mk() {
        return row("MADHYASTHA_KARYALAYA", null, null, null, null);
    }

    /** Zone scope: owns the Kshetras beneath it (ADR-0009). */
    Callers sanyojakOf(UUID zoneId) {
        return row("SANYOJAK", null, null, zoneId, null);
    }

    /**
     * City scope: owns the Zones beneath it (ADR-0024). The row carries a
     * demographic, as every Regional Team appointment does — and the Zone-creation
     * question deliberately ignores it, which is the asymmetry worth having a
     * real row to demonstrate.
     */
    Callers regionalTeamIn(UUID cityId) {
        return row("REGIONAL_TEAM", null, null, null, cityId).demographic("YUVAK");
    }

    /** Kshetra scope: owns the Sabhas beneath it (ADR-0011, ADR-0026). */
    Callers nirdeshakOf(UUID kshetraId, String demographic) {
        return row("NIRDESHAK", null, kshetraId, null, null).demographic(demographic);
    }

    CallerAuthority build() {
        return new CallerAuthority(UserId.of(userId), rows);
    }

    private Callers row(String role, UUID sabhaId, UUID kshetraId, UUID zoneId, UUID cityId) {
        rows.add(new RoleAssignment(role, sabhaId, kshetraId, zoneId, cityId, null));
        return this;
    }

    private Callers demographic(String demographic) {
        RoleAssignment last = rows.remove(rows.size() - 1);
        rows.add(new RoleAssignment(last.role(), last.sabhaId(), last.kshetraId(),
                last.zoneId(), last.cityId(), demographic));
        return this;
    }
}
