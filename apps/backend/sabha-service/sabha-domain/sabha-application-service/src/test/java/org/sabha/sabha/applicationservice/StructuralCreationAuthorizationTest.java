package org.sabha.sabha.applicationservice;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StructuralCreationAuthorizationTest {

    private static final UUID MK = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID SANYOJAK = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    private static final UUID OUTSIDER = UUID.fromString("00000000-0000-0000-0000-0000000000cc");
    private static final UUID REGIONAL_TEAM = UUID.fromString("00000000-0000-0000-0000-0000000000dd");
    private static final UUID ZONE = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID OTHER_ZONE = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID CITY = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID OTHER_CITY = UUID.fromString("00000000-0000-0000-0000-0000000000c2");

    private final StructuralCreationAuthorization authz = new StructuralCreationAuthorization(
            userId -> userId.equals(MK),
            userId -> userId.equals(SANYOJAK) ? List.of(ZONE) : List.of(),
            userId -> userId.equals(REGIONAL_TEAM) ? List.of(CITY) : List.of());

    @Test
    void mkMemberMayCreateStateStructure() {
        assertThat(authz.canCreateStateStructure(MK)).isTrue();
    }

    @Test
    void nonMkMayNotCreateStateStructure() {
        assertThat(authz.canCreateStateStructure(SANYOJAK)).isFalse();
        assertThat(authz.canCreateStateStructure(OUTSIDER)).isFalse();
    }

    @Test
    void regionalTeamMemberMayCreateAZoneWithinTheirOwnCity() {
        assertThat(authz.canCreateZone(REGIONAL_TEAM, CITY)).isTrue();
    }

    @Test
    void regionalTeamMemberMayNotCreateAZoneInACityOutsideTheirScope() {
        assertThat(authz.canCreateZone(REGIONAL_TEAM, OTHER_CITY)).isFalse();
    }

    @Test
    void mkMembershipDoesNotConferZoneCreation() {
        // Zone creation moved MK -> Regional Team (ADR-0024); MK has no path at all.
        assertThat(authz.canCreateZone(MK, CITY)).isFalse();
    }

    @Test
    void nonRegionalTeamMayNotCreateAZone() {
        assertThat(authz.canCreateZone(OUTSIDER, CITY)).isFalse();
    }

    @Test
    void sanyojakMayCreateAKshetraWithinTheirOwnZone() {
        assertThat(authz.canCreateKshetra(SANYOJAK, ZONE)).isTrue();
    }

    @Test
    void sanyojakMayNotCreateAKshetraInAZoneOutsideTheirScope() {
        assertThat(authz.canCreateKshetra(SANYOJAK, OTHER_ZONE)).isFalse();
    }

    @Test
    void nonSanyojakMayNotCreateAKshetra() {
        assertThat(authz.canCreateKshetra(OUTSIDER, ZONE)).isFalse();
    }

    @Test
    void mkMembershipAloneDoesNotConferKshetraCreation() {
        // Kshetra creation is Sanyojak authority — MK is a separate tier (ADR-0009).
        assertThat(authz.canCreateKshetra(MK, ZONE)).isFalse();
    }
}
