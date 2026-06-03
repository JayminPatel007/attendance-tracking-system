package org.sabha.identity.applicationservice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.SabhaScope;
import org.sabha.common.StructuralHierarchyLookup;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Authorization Engine for role appointment (ADR-0011). Pure decision
 * component: given an appointer and the scope of the role being filled, it
 * resolves the geographic containment and answers whether the appointer holds
 * the appointing tier. It never throws or mutates — the application service
 * turns a {@code false} into an {@link org.sabha.common.AuthorizationDeniedException}.
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
        f.hierarchy.sabhaScopes.put(SABHA, new SabhaScope(KSHETRA, YUVAK, "REGULAR"));
        f.authority.nirdeshakScopes.add(scope(NIRDESHAK, KSHETRA, YUVAK));

        boolean allowed = f.engine().canAppoint(NIRDESHAK,
                AppointmentScope.onSabha(AppointableRole.SANCHALAK, SABHA));

        assertThat(allowed).isTrue();
    }

    @Test
    void nirdeshakMayNotAppointSanchalakOnASabhaInAnotherKshetra() {
        Fixture f = new Fixture();
        // The Sabha sits in a Kshetra/demographic this Nirdeshak does not hold.
        f.hierarchy.sabhaScopes.put(SABHA, new SabhaScope(KSHETRA, YUVAK, "REGULAR"));
        f.authority.nirdeshakScopes.add(scope(NIRDESHAK, OTHER_KSHETRA, YUVAK));

        boolean allowed = f.engine().canAppoint(NIRDESHAK,
                AppointmentScope.onSabha(AppointableRole.SANCHALAK, SABHA));

        assertThat(allowed).isFalse();
    }

    @Test
    void nirdeshakMayAppointSahSanchalakOnASabhaWithinScope() {
        Fixture f = new Fixture();
        f.hierarchy.sabhaScopes.put(SABHA, new SabhaScope(KSHETRA, YUVAK, "REGULAR"));
        f.authority.nirdeshakScopes.add(scope(NIRDESHAK, KSHETRA, YUVAK));

        boolean allowed = f.engine().canAppoint(NIRDESHAK,
                AppointmentScope.onSabha(AppointableRole.SAH_SANCHALAK, SABHA));

        assertThat(allowed).isTrue();
    }

    @Test
    void nirdeshakMayAppointNirikshakAndSahNirdeshakWithinTheirKshetraAndDemographic() {
        Fixture f = new Fixture();
        f.authority.nirdeshakScopes.add(scope(NIRDESHAK, KSHETRA, YUVAK));

        assertThat(f.engine().canAppoint(NIRDESHAK,
                AppointmentScope.onKshetra(AppointableRole.NIRIKSHAK, KSHETRA, YUVAK))).isTrue();
        assertThat(f.engine().canAppoint(NIRDESHAK,
                AppointmentScope.onKshetra(AppointableRole.SAH_NIRDESHAK, KSHETRA, YUVAK))).isTrue();
    }

    @Test
    void nirdeshakMayNotAppointAKshetraRoleForADemographicTheyDoNotHold() {
        Fixture f = new Fixture();
        f.authority.nirdeshakScopes.add(scope(NIRDESHAK, KSHETRA, YUVAK));

        assertThat(f.engine().canAppoint(NIRDESHAK,
                AppointmentScope.onKshetra(AppointableRole.NIRIKSHAK, KSHETRA, BAAL))).isFalse();
    }

    @Test
    void sanyojakMayAppointNirdeshakInAKshetraWithinTheirZoneAndDemographic() {
        Fixture f = new Fixture();
        f.hierarchy.zoneOfKshetra.put(KSHETRA, ZONE);
        f.authority.sanyojakScopes.add(scope(SANYOJAK, ZONE, YUVAK));

        boolean allowed = f.engine().canAppoint(SANYOJAK,
                AppointmentScope.onKshetra(AppointableRole.NIRDESHAK, KSHETRA, YUVAK));

        assertThat(allowed).isTrue();
    }

    @Test
    void sanyojakMayNotAppointNirdeshakInAKshetraOutsideTheirZone() {
        Fixture f = new Fixture();
        f.hierarchy.zoneOfKshetra.put(KSHETRA, ZONE);
        f.authority.sanyojakScopes.add(scope(SANYOJAK, OTHER_ZONE, YUVAK));

        boolean allowed = f.engine().canAppoint(SANYOJAK,
                AppointmentScope.onKshetra(AppointableRole.NIRDESHAK, KSHETRA, YUVAK));

        assertThat(allowed).isFalse();
    }

    @Test
    void regionalTeamMayAppointSanyojakInAZoneWithinTheirCityAndDemographic() {
        Fixture f = new Fixture();
        f.hierarchy.cityOfZone.put(ZONE, CITY);
        f.authority.regionalTeamScopes.add(scope(REGIONAL, CITY, YUVAK));

        boolean allowed = f.engine().canAppoint(REGIONAL,
                AppointmentScope.onZone(AppointableRole.SANYOJAK, ZONE, YUVAK));

        assertThat(allowed).isTrue();
    }

    @Test
    void regionalTeamMayNotAppointSanyojakInAZoneOutsideTheirCity() {
        Fixture f = new Fixture();
        f.hierarchy.cityOfZone.put(ZONE, CITY);
        f.authority.regionalTeamScopes.add(scope(REGIONAL, OTHER_CITY, YUVAK));

        boolean allowed = f.engine().canAppoint(REGIONAL,
                AppointmentScope.onZone(AppointableRole.SANYOJAK, ZONE, YUVAK));

        assertThat(allowed).isFalse();
    }

    @Test
    void madhyasthaKaryalayaMayAppointRegionalTeamMembersAndCreateSants() {
        Fixture f = new Fixture();
        f.mkMember = MK;

        assertThat(f.engine().canAppoint(MK,
                AppointmentScope.onCity(AppointableRole.REGIONAL_TEAM, CITY, YUVAK))).isTrue();
        assertThat(f.engine().canAppoint(MK,
                AppointmentScope.onCity(AppointableRole.SANT, CITY, YUVAK))).isTrue();
    }

    @Test
    void nonMkMayNotAppointRegionalTeamMembersOrCreateSants() {
        Fixture f = new Fixture();
        f.mkMember = MK;

        assertThat(f.engine().canAppoint(OUTSIDER,
                AppointmentScope.onCity(AppointableRole.REGIONAL_TEAM, CITY, YUVAK))).isFalse();
        assertThat(f.engine().canAppoint(OUTSIDER,
                AppointmentScope.onCity(AppointableRole.SANT, CITY, YUVAK))).isFalse();
    }

    @Test
    void aSanyojakMayNotReachDownAndAppointASanchalakDirectly() {
        // Wrong tier: Sanchalak is the Nirdeshak's to appoint, not the Sanyojak's.
        Fixture f = new Fixture();
        f.hierarchy.sabhaScopes.put(SABHA, new SabhaScope(KSHETRA, YUVAK, "REGULAR"));
        f.authority.sanyojakScopes.add(scope(SANYOJAK, ZONE, YUVAK));

        boolean allowed = f.engine().canAppoint(SANYOJAK,
                AppointmentScope.onSabha(AppointableRole.SANCHALAK, SABHA));

        assertThat(allowed).isFalse();
    }

    private static String scope(UUID user, UUID scopeId, String demographic) {
        return user + "|" + scopeId + "|" + demographic;
    }

    private static final class Fixture {
        final FakeHierarchy hierarchy = new FakeHierarchy();
        final FakeAuthority authority = new FakeAuthority();
        UUID mkMember;

        AppointmentAuthorization engine() {
            return new AppointmentAuthorization(hierarchy, authority,
                    userId -> userId.equals(mkMember));
        }
    }

    private static final class FakeHierarchy implements StructuralHierarchyLookup {
        final Map<UUID, SabhaScope> sabhaScopes = new HashMap<>();
        final Map<UUID, UUID> zoneOfKshetra = new HashMap<>();
        final Map<UUID, UUID> cityOfZone = new HashMap<>();

        @Override
        public Optional<SabhaScope> sabhaScope(UUID sabhaId) {
            return Optional.ofNullable(sabhaScopes.get(sabhaId));
        }

        @Override
        public Optional<UUID> zoneOfKshetra(UUID kshetraId) {
            return Optional.ofNullable(zoneOfKshetra.get(kshetraId));
        }

        @Override
        public Optional<UUID> cityOfZone(UUID zoneId) {
            return Optional.ofNullable(cityOfZone.get(zoneId));
        }
    }

    private static final class FakeAuthority implements AppointerAuthorityLookup {
        final Set<String> nirdeshakScopes = new HashSet<>();
        final Set<String> sanyojakScopes = new HashSet<>();
        final Set<String> regionalTeamScopes = new HashSet<>();

        @Override
        public boolean holdsNirdeshak(UUID userId, UUID kshetraId, String demographic) {
            return nirdeshakScopes.contains(userId + "|" + kshetraId + "|" + demographic);
        }

        @Override
        public boolean holdsSanyojak(UUID userId, UUID zoneId, String demographic) {
            return sanyojakScopes.contains(userId + "|" + zoneId + "|" + demographic);
        }

        @Override
        public boolean holdsRegionalTeam(UUID userId, UUID cityId, String demographic) {
            return regionalTeamScopes.contains(userId + "|" + cityId + "|" + demographic);
        }
    }
}
