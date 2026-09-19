package org.sabha.analytics.applicationservice;

import java.util.List;
import java.util.UUID;

import org.sabha.common.CallerAuthority;
import org.sabha.common.RoleAssignment;
import org.sabha.common.UserId;

/**
 * Builds the {@link CallerAuthority} literals analytics' tests read.
 *
 * <p>It replaces {@code FakeSantLookup}, which every dashboard test had to stand
 * up to say "this caller is a Sant". All three of its call sites asked about the
 * <em>caller</em>, so ADR-0032 folded them onto the caller parameter and analytics
 * stopped depending on the port entirely. {@code SantLookup} itself survives — for
 * the one site, in identity, that asks whether the <em>target</em> is a Sant.</p>
 */
final class Callers {

    private Callers() {
    }

    /** A caller holding no role — the ordinary Karyakar, and the denied case. */
    static CallerAuthority noRoles(UUID userId) {
        return CallerAuthority.withNoRoles(UserId.of(userId));
    }

    /** A Sant: universal read, recorded as a {@code role_assignments} row (ADR-0029). */
    static CallerAuthority sant(UUID userId) {
        return row(userId, "SANT", null, null, null, null);
    }

    static CallerAuthority madhyasthaKaryalaya(UUID userId) {
        return row(userId, "MADHYASTHA_KARYALAYA", null, null, null, null);
    }

    static CallerAuthority nirdeshakOf(UUID userId, UUID kshetraId, String demographic) {
        return row(userId, "NIRDESHAK", kshetraId, null, null, demographic);
    }

    static CallerAuthority regionalTeamIn(UUID userId, UUID cityId, String demographic) {
        return row(userId, "REGIONAL_TEAM", null, null, cityId, demographic);
    }

    static CallerAuthority sanchalakOf(UUID userId, UUID sabhaId) {
        return new CallerAuthority(UserId.of(userId),
                List.of(new RoleAssignment("SANCHALAK", sabhaId, null, null, null, null)));
    }

    private static CallerAuthority row(UUID userId, String role, UUID kshetraId,
                                       UUID zoneId, UUID cityId, String demographic) {
        return new CallerAuthority(UserId.of(userId),
                List.of(new RoleAssignment(role, null, kshetraId, zoneId, cityId, demographic)));
    }
}
