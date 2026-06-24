package org.sabha.sabha.applicationservice;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.Role;

import static org.assertj.core.api.Assertions.assertThat;

class StructuralScopeAuthorityTest {

    private static final UUID MK = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID SANYOJAK = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    private static final UUID OUTSIDER = UUID.fromString("00000000-0000-0000-0000-0000000000cc");
    private static final UUID REGIONAL_TEAM = UUID.fromString("00000000-0000-0000-0000-0000000000dd");
    private static final UUID NIRDESHAK = UUID.fromString("00000000-0000-0000-0000-0000000000ee");
    private static final UUID ZONE = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID OTHER_ZONE = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID CITY = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID OTHER_CITY = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final UUID KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final String YUVAK = "YUVAK";

    private final FakeRoleAssignments roleAssignments = new FakeRoleAssignments();

    private final StructuralScopeAuthority authz = new StructuralScopeAuthority(
            userId -> userId.equals(MK),
            userId -> userId.equals(SANYOJAK) ? List.of(ZONE) : List.of(),
            userId -> userId.equals(REGIONAL_TEAM) ? List.of(CITY) : List.of(),
            roleAssignments);

    {
        roleAssignments.grant(NIRDESHAK, KSHETRA, YUVAK, Role.NIRDESHAK);
    }

    @Test
    void mkMemberHoldsStateScope() {
        assertThat(authz.holdsStateScope(MK)).isTrue();
    }

    @Test
    void nonMkDoesNotHoldStateScope() {
        assertThat(authz.holdsStateScope(SANYOJAK)).isFalse();
        assertThat(authz.holdsStateScope(OUTSIDER)).isFalse();
    }

    @Test
    void regionalTeamMemberHoldsTheirOwnCityScope() {
        assertThat(authz.holdsCityScope(REGIONAL_TEAM, CITY)).isTrue();
    }

    @Test
    void regionalTeamMemberDoesNotHoldAnotherCityScope() {
        assertThat(authz.holdsCityScope(REGIONAL_TEAM, OTHER_CITY)).isFalse();
    }

    @Test
    void stateScopeDoesNotConferCityScope() {
        // Zone authority moved MK -> Regional Team (ADR-0024); MK has no City scope.
        assertThat(authz.holdsCityScope(MK, CITY)).isFalse();
    }

    @Test
    void anOutsiderHoldsNoCityScope() {
        assertThat(authz.holdsCityScope(OUTSIDER, CITY)).isFalse();
    }

    @Test
    void sanyojakHoldsTheirOwnZoneScope() {
        assertThat(authz.holdsZoneScope(SANYOJAK, ZONE)).isTrue();
    }

    @Test
    void sanyojakDoesNotHoldAnotherZoneScope() {
        assertThat(authz.holdsZoneScope(SANYOJAK, OTHER_ZONE)).isFalse();
    }

    @Test
    void anOutsiderHoldsNoZoneScope() {
        assertThat(authz.holdsZoneScope(OUTSIDER, ZONE)).isFalse();
    }

    @Test
    void stateScopeAloneDoesNotConferZoneScope() {
        // Kshetra authority is the Zone's Sanyojak — MK is a separate tier (ADR-0009).
        assertThat(authz.holdsZoneScope(MK, ZONE)).isFalse();
    }

    @Test
    void nirdeshakHoldsTheirOwnKshetraScope() {
        assertThat(authz.holdsKshetraScope(NIRDESHAK, KSHETRA, YUVAK)).isTrue();
    }

    @Test
    void nirdeshakOfAnotherDemographicDoesNotHoldThisKshetraScope() {
        assertThat(authz.holdsKshetraScope(NIRDESHAK, KSHETRA, "BAAL")).isFalse();
    }

    @Test
    void anOutsiderHoldsNoKshetraScope() {
        assertThat(authz.holdsKshetraScope(OUTSIDER, KSHETRA, YUVAK)).isFalse();
    }
}
