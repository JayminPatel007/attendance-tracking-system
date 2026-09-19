package org.sabha.analytics.applicationservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.CallerAuthority;
import org.sabha.common.RoleAssignment;
import org.sabha.common.UserId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit behaviours of the audit-log Authorization Engine (ADR-0023, Slice 19): the
 * State-wide oversight bodies read everything, the Kshetra/Zone/City tiers read
 * their resolved geography, and every tier below Nirdeshak is denied.
 *
 * <p>It has no ports at all since ADR-0032 — its three fakes were all answering
 * questions about the caller's own {@code role_assignments}, and the caller now
 * arrives holding them. The engine is kept rather than deleted because the
 * deletion rule needs zero ports <em>and</em> zero policy, and the tier fold plus
 * {@code empty ⇒ Denied} below are exactly the policy that survives.</p>
 */
class AuditLogAccessTest {

    private static final UUID CALLER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID ZONE = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID CITY = UUID.fromString("00000000-0000-0000-0000-0000000000b3");
    private static final String YUVAK = "YUVAK";

    private final AuditLogAccess access = new AuditLogAccess();

    @Test
    void aMadhyasthaKaryalayaMemberReadsTheWholeState() {
        assertThat(access.scopeFor(rows().mk().build())).isEqualTo(new AuditScope.Unrestricted());
    }

    @Test
    void aSantReadsTheWholeStateViaTheUniversalReadException() {
        assertThat(access.scopeFor(rows().sant().build())).isEqualTo(new AuditScope.Unrestricted());
    }

    @Test
    void aNirdeshakReadsTheirResolvedKshetraScope() {
        assertThat(access.scopeFor(rows().nirdeshakIn(KSHETRA, YUVAK).build()))
                .isEqualTo(new AuditScope.Scoped(Set.of(KSHETRA), Set.of(), Set.of()));
    }

    @Test
    void aSahNirdeshakReadsTheirKshetraToo() {
        // ADR-0023 makes the oversight read broader than write authority: the
        // Sah-Nirdeshak cannot appoint in the Kshetra but can audit it.
        assertThat(access.scopeFor(rows().sahNirdeshakIn(KSHETRA, YUVAK).build()))
                .isEqualTo(new AuditScope.Scoped(Set.of(KSHETRA), Set.of(), Set.of()));
    }

    @Test
    void theAuditReadIsNotDemographicFiltered() {
        // The same row answers for any demographic — deliberately unlike the
        // appointment-authority reading of these rows (ADR-0023 vs ADR-0011).
        assertThat(access.scopeFor(rows().nirdeshakIn(KSHETRA, "BAAL").build()))
                .isEqualTo(new AuditScope.Scoped(Set.of(KSHETRA), Set.of(), Set.of()));
    }

    @Test
    void aSanyojakReadsTheirResolvedZoneScope() {
        assertThat(access.scopeFor(rows().sanyojakOf(ZONE).build()))
                .isEqualTo(new AuditScope.Scoped(Set.of(), Set.of(ZONE), Set.of()));
    }

    @Test
    void aRegionalTeamMemberReadsTheirResolvedCityScope() {
        // The City-scoped Regional Team is admitted just like the Kshetra/Zone tiers,
        // even though it is not an operational Role — the engine folds purely on the
        // resolved geographic scope (issue #80, ADR-0023).
        assertThat(access.scopeFor(rows().regionalTeamIn(CITY, YUVAK).build()))
                .isEqualTo(new AuditScope.Scoped(Set.of(), Set.of(), Set.of(CITY)));
    }

    @Test
    void aTierBelowNirdeshakResolvesToNoScopeAndIsDenied() {
        // A Sanchalak holds no Nirdeshak-and-above geographic row, so every set comes
        // back empty and the engine folds that to Denied — the forbidden tiers are
        // never enumerated.
        CallerAuthority sanchalak = rows()
                .row("SANCHALAK", UUID.randomUUID(), null, null, null, null)
                .build();

        assertThat(access.scopeFor(sanchalak)).isEqualTo(new AuditScope.Denied());
    }

    @Test
    void anUnknownCallerIsDenied() {
        assertThat(access.scopeFor(CallerAuthority.withNoRoles(UserId.of(UUID.randomUUID()))))
                .isEqualTo(new AuditScope.Denied());
    }

    @Test
    void deniedIsTheOneScopeThatIsNotAdmitted() {
        // The single admission rule both the sidebar and the BFF read (ADR-0032).
        assertThat(access.scopeFor(rows().mk().build()).admitted()).isTrue();
        assertThat(access.scopeFor(rows().sanyojakOf(ZONE).build()).admitted()).isTrue();
        assertThat(access.scopeFor(rows().build()).admitted()).isFalse();
    }

    private static Rows rows() {
        return new Rows();
    }

    /** Literal {@code role_assignments} rows — the engine holds no port to fake. */
    private static final class Rows {
        private final List<RoleAssignment> rows = new ArrayList<>();

        Rows mk() {
            return row("MADHYASTHA_KARYALAYA", null, null, null, null, null);
        }

        Rows sant() {
            return row("SANT", null, null, null, null, null);
        }

        Rows nirdeshakIn(UUID kshetraId, String demographic) {
            return row("NIRDESHAK", null, kshetraId, null, null, demographic);
        }

        Rows sahNirdeshakIn(UUID kshetraId, String demographic) {
            return row("SAH_NIRDESHAK", null, kshetraId, null, null, demographic);
        }

        Rows sanyojakOf(UUID zoneId) {
            return row("SANYOJAK", null, null, zoneId, null, null);
        }

        Rows regionalTeamIn(UUID cityId, String demographic) {
            return row("REGIONAL_TEAM", null, null, null, cityId, demographic);
        }

        Rows row(String role, UUID sabhaId, UUID kshetraId, UUID zoneId, UUID cityId, String demographic) {
            rows.add(new RoleAssignment(role, sabhaId, kshetraId, zoneId, cityId, demographic));
            return this;
        }

        CallerAuthority build() {
            return new CallerAuthority(UserId.of(CALLER), rows);
        }
    }
}
