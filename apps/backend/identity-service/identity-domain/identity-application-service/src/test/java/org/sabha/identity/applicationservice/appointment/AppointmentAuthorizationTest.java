package org.sabha.identity.applicationservice.appointment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.CallerAuthority;
import org.sabha.common.RoleAssignment;
import org.sabha.common.SabhaScope;
import org.sabha.common.SabhaFact;
import org.sabha.common.SabhaFacts;
import org.sabha.common.StructuralParentage;
import org.sabha.common.UserId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Authorization Engine for role appointment (ADR-0011). Pure decision
 * component: given an appointer and the scope of the role being filled, it
 * resolves the geographic containment and answers whether the appointer holds
 * the appointing tier. It never throws or mutates — the application service
 * turns a {@code false} into an {@link org.sabha.common.AuthorizationDeniedException}.
 *
 * <p>Since ADR-0032 the appointer arrives as a {@link CallerAuthority} literal
 * rather than through a string-keyed fake of {@code AppointerAuthorityLookup}:
 * the two ports left are the ones that resolve containment, and they are keyed by
 * the target scope. Building the appointer's rows directly is what makes a test
 * like "an RT member may not appoint a peer for another demographic" read as the
 * one row it is about.</p>
 */
class AppointmentAuthorizationTest {

    private static final String YUVAK = "YUVAK";
    private static final String BAAL = "BAAL";

    private static final UUID NIRDESHAK = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID SANYOJAK = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID REGIONAL = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
    private static final UUID MK = UUID.fromString("00000000-0000-0000-0000-0000000000a4");
    private static final UUID OUTSIDER = UUID.fromString("00000000-0000-0000-0000-0000000000a5");
    private static final UUID SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID OTHER_KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final UUID ZONE = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final UUID OTHER_ZONE = UUID.fromString("00000000-0000-0000-0000-0000000000d2");
    private static final UUID CITY = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final UUID OTHER_CITY = UUID.fromString("00000000-0000-0000-0000-0000000000e2");

    @Test
    void nirdeshakMayAppointSanchalakOnASabhaWithinTheirKshetraAndDemographic() {
        Fixture f = new Fixture();
        f.sabhaFacts.sabhaScopes.put(SABHA, new SabhaScope(KSHETRA, YUVAK, "REGULAR"));
        f.authority.nirdeshakIn(KSHETRA, YUVAK);

        boolean allowed = f.engine().canAppoint(f.caller(NIRDESHAK),
                AppointmentScope.onSabha(AppointableRole.SANCHALAK, SABHA));

        assertThat(allowed).isTrue();
    }

    @Test
    void nirdeshakMayNotAppointSanchalakOnASabhaInAnotherKshetra() {
        Fixture f = new Fixture();
        // The Sabha sits in a Kshetra/demographic this Nirdeshak does not hold.
        f.sabhaFacts.sabhaScopes.put(SABHA, new SabhaScope(KSHETRA, YUVAK, "REGULAR"));
        f.authority.nirdeshakIn(OTHER_KSHETRA, YUVAK);

        boolean allowed = f.engine().canAppoint(f.caller(NIRDESHAK),
                AppointmentScope.onSabha(AppointableRole.SANCHALAK, SABHA));

        assertThat(allowed).isFalse();
    }

    @Test
    void nirdeshakMayAppointSahSanchalakOnASabhaWithinScope() {
        Fixture f = new Fixture();
        f.sabhaFacts.sabhaScopes.put(SABHA, new SabhaScope(KSHETRA, YUVAK, "REGULAR"));
        f.authority.nirdeshakIn(KSHETRA, YUVAK);

        boolean allowed = f.engine().canAppoint(f.caller(NIRDESHAK),
                AppointmentScope.onSabha(AppointableRole.SAH_SANCHALAK, SABHA));

        assertThat(allowed).isTrue();
    }

    @Test
    void nirdeshakMayAppointNirikshakAndSahNirdeshakWithinTheirKshetraAndDemographic() {
        Fixture f = new Fixture();
        f.authority.nirdeshakIn(KSHETRA, YUVAK);

        assertThat(f.engine().canAppoint(f.caller(NIRDESHAK),
                AppointmentScope.onKshetra(AppointableRole.NIRIKSHAK, KSHETRA, YUVAK))).isTrue();
        assertThat(f.engine().canAppoint(f.caller(NIRDESHAK),
                AppointmentScope.onKshetra(AppointableRole.SAH_NIRDESHAK, KSHETRA, YUVAK))).isTrue();
    }

    @Test
    void nirdeshakMayNotAppointAKshetraRoleForADemographicTheyDoNotHold() {
        Fixture f = new Fixture();
        f.authority.nirdeshakIn(KSHETRA, YUVAK);

        assertThat(f.engine().canAppoint(f.caller(NIRDESHAK),
                AppointmentScope.onKshetra(AppointableRole.NIRIKSHAK, KSHETRA, BAAL))).isFalse();
    }

    @Test
    void sanyojakMayAppointNirdeshakInAKshetraWithinTheirZoneAndDemographic() {
        Fixture f = new Fixture();
        f.parentage.zoneOfKshetra.put(KSHETRA, ZONE);
        f.authority.sanyojakIn(ZONE, YUVAK);

        boolean allowed = f.engine().canAppoint(f.caller(SANYOJAK),
                AppointmentScope.onKshetra(AppointableRole.NIRDESHAK, KSHETRA, YUVAK));

        assertThat(allowed).isTrue();
    }

    @Test
    void sanyojakMayNotAppointNirdeshakInAKshetraOutsideTheirZone() {
        Fixture f = new Fixture();
        f.parentage.zoneOfKshetra.put(KSHETRA, ZONE);
        f.authority.sanyojakIn(OTHER_ZONE, YUVAK);

        boolean allowed = f.engine().canAppoint(f.caller(SANYOJAK),
                AppointmentScope.onKshetra(AppointableRole.NIRDESHAK, KSHETRA, YUVAK));

        assertThat(allowed).isFalse();
    }

    @Test
    void regionalTeamMayAppointSanyojakInAZoneWithinTheirCityAndDemographic() {
        Fixture f = new Fixture();
        f.parentage.cityOfZone.put(ZONE, CITY);
        f.authority.regionalTeamIn(CITY, YUVAK);

        boolean allowed = f.engine().canAppoint(f.caller(REGIONAL),
                AppointmentScope.onZone(AppointableRole.SANYOJAK, ZONE, YUVAK));

        assertThat(allowed).isTrue();
    }

    @Test
    void regionalTeamMayNotAppointSanyojakInAZoneOutsideTheirCity() {
        Fixture f = new Fixture();
        f.parentage.cityOfZone.put(ZONE, CITY);
        f.authority.regionalTeamIn(OTHER_CITY, YUVAK);

        boolean allowed = f.engine().canAppoint(f.caller(REGIONAL),
                AppointmentScope.onZone(AppointableRole.SANYOJAK, ZONE, YUVAK));

        assertThat(allowed).isFalse();
    }

    @Test
    void madhyasthaKaryalayaMayAppointRegionalTeamMembersAndCreateSants() {
        Fixture f = new Fixture();
        f.authority.madhyasthaKaryalaya();

        assertThat(f.engine().canAppoint(f.caller(MK),
                AppointmentScope.onCity(AppointableRole.REGIONAL_TEAM, CITY, YUVAK))).isTrue();
        assertThat(f.engine().canAppoint(f.caller(MK),
                AppointmentScope.onCity(AppointableRole.SANT, CITY, YUVAK))).isTrue();
    }

    @Test
    void aRegionalTeamMemberMayAppointAPeerInTheSameCityAndDemographic() {
        // Self-replication (ADR-0025 §2): an existing RT member of (City, Baal)
        // appoints another RT member for (City, Baal) — no MK round-trip.
        Fixture f = new Fixture();
        f.authority.regionalTeamIn(CITY, BAAL);

        boolean allowed = f.engine().canAppoint(f.caller(REGIONAL),
                AppointmentScope.onCity(AppointableRole.REGIONAL_TEAM, CITY, BAAL));

        assertThat(allowed).isTrue();
    }

    @Test
    void aRegionalTeamMemberMayNotAppointAPeerInAnotherCity() {
        // RT authority is bound to (City, demographic): holding (OTHER_CITY, Baal)
        // grants nothing over (CITY, Baal).
        Fixture f = new Fixture();
        f.authority.regionalTeamIn(OTHER_CITY, BAAL);

        boolean allowed = f.engine().canAppoint(f.caller(REGIONAL),
                AppointmentScope.onCity(AppointableRole.REGIONAL_TEAM, CITY, BAAL));

        assertThat(allowed).isFalse();
    }

    @Test
    void aRegionalTeamMemberMayNotAppointAPeerForAnotherDemographic() {
        // RT authority does not cross demographics: a (CITY, Yuvak) member cannot
        // appoint into (CITY, Baal).
        Fixture f = new Fixture();
        f.authority.regionalTeamIn(CITY, YUVAK);

        boolean allowed = f.engine().canAppoint(f.caller(REGIONAL),
                AppointmentScope.onCity(AppointableRole.REGIONAL_TEAM, CITY, BAAL));

        assertThat(allowed).isFalse();
    }

    @Test
    void aRegionalTeamMemberMayNotCreateASant() {
        // Self-replication is RT-only; Sant remains an MK-only administrative act.
        Fixture f = new Fixture();
        f.authority.regionalTeamIn(CITY, BAAL);

        boolean allowed = f.engine().canAppoint(f.caller(REGIONAL),
                AppointmentScope.onCity(AppointableRole.SANT, CITY, BAAL));

        assertThat(allowed).isFalse();
    }

    @Test
    void nonMkMayNotAppointRegionalTeamMembersOrCreateSants() {
        // The outsider holds no row at all — which under the fold is simply an
        // empty authority, not a fake configured to say no.
        Fixture f = new Fixture();

        assertThat(f.engine().canAppoint(f.caller(OUTSIDER),
                AppointmentScope.onCity(AppointableRole.REGIONAL_TEAM, CITY, YUVAK))).isFalse();
        assertThat(f.engine().canAppoint(f.caller(OUTSIDER),
                AppointmentScope.onCity(AppointableRole.SANT, CITY, YUVAK))).isFalse();
    }

    @Test
    void aSanyojakMayNotReachDownAndAppointASanchalakDirectly() {
        // Wrong tier: Sanchalak is the Nirdeshak's to appoint, not the Sanyojak's.
        Fixture f = new Fixture();
        f.sabhaFacts.sabhaScopes.put(SABHA, new SabhaScope(KSHETRA, YUVAK, "REGULAR"));
        f.authority.sanyojakIn(ZONE, YUVAK);

        boolean allowed = f.engine().canAppoint(f.caller(SANYOJAK),
                AppointmentScope.onSabha(AppointableRole.SANCHALAK, SABHA));

        assertThat(allowed).isFalse();
    }

    private static final class Fixture {
        final FakeSabhaFacts sabhaFacts = new FakeSabhaFacts();
        final FakeParentage parentage = new FakeParentage();
        final Rows authority = new Rows();

        AppointmentAuthorization engine() {
            return new AppointmentAuthorization(sabhaFacts, parentage);
        }

        CallerAuthority caller(UUID userId) {
            return new CallerAuthority(UserId.of(userId), authority.rows);
        }
    }

    /**
     * The fake that stopped being one. These are literal {@code role_assignments}
     * rows, spelled the way the single adapter reads them — the scope column each
     * tier uses is part of what the test is asserting.
     */
    private static final class Rows {
        final List<RoleAssignment> rows = new ArrayList<>();

        void nirdeshakIn(UUID kshetraId, String demographic) {
            rows.add(new RoleAssignment("NIRDESHAK", null, kshetraId, null, null, demographic));
        }

        void sanyojakIn(UUID zoneId, String demographic) {
            rows.add(new RoleAssignment("SANYOJAK", null, null, zoneId, null, demographic));
        }

        void regionalTeamIn(UUID cityId, String demographic) {
            rows.add(new RoleAssignment("REGIONAL_TEAM", null, null, null, cityId, demographic));
        }

        /** State-level: a null-scope row, which is exactly how MK is stored (ADR-0005). */
        void madhyasthaKaryalaya() {
            rows.add(new RoleAssignment("MADHYASTHA_KARYALAYA", null, null, null, null, null));
        }
    }

    private static final class FakeSabhaFacts implements SabhaFacts {
        final Map<UUID, SabhaScope> sabhaScopes = new HashMap<>();

        @Override
        public Optional<SabhaFact> of(UUID sabhaId) {
            return Optional.ofNullable(sabhaScopes.get(sabhaId))
                    .map(scope -> SabhaFact.monthlyAdHoc(sabhaId, scope, false));
        }

        @Override
        public List<SabhaFact> allWeekly() {
            return List.of();
        }

        @Override
        public Optional<SabhaFact> selectiveIn(UUID kshetraId, String demographic, String track) {
            return Optional.empty();
        }
    }

    private static final class FakeParentage implements StructuralParentage {
        final Map<UUID, UUID> zoneOfKshetra = new HashMap<>();
        final Map<UUID, UUID> cityOfZone = new HashMap<>();

        @Override
        public Optional<UUID> zoneOfKshetra(UUID kshetraId) {
            return Optional.ofNullable(zoneOfKshetra.get(kshetraId));
        }

        @Override
        public Optional<UUID> cityOfZone(UUID zoneId) {
            return Optional.ofNullable(cityOfZone.get(zoneId));
        }
    }
}
