package org.sabha.common;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The caller's own {@code role_assignments}, read as named domain questions.
 *
 * <p>Most of this class arrived from {@code StructuralScopeAuthorityTest}, whose
 * engine ADR-0032 deleted for holding no policy. The tests came with the tier
 * table: the structural create and delete paths still read these four methods, so
 * the assertions that stop them drifting had to keep existing somewhere, and the
 * value type is where the rule now lives. They also got cheaper — each one used
 * to need four test doubles wired into an engine constructor, and now needs a
 * row.</p>
 */
class CallerAuthorityTest {

    private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID ZONE = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID OTHER_ZONE = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID CITY = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID OTHER_CITY = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final UUID KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final UUID SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final String YUVAK = "YUVAK";
    private static final String BAAL = "BAAL";

    // --- State scope: owns Cities and Sabha Kinds ---

    @Test
    void mkMemberHoldsStateScope() {
        assertThat(rows().mk().build().isMadhyasthaKaryalaya()).isTrue();
    }

    @Test
    void nonMkDoesNotHoldStateScope() {
        assertThat(rows().sanyojakOf(ZONE).build().isMadhyasthaKaryalaya()).isFalse();
        assertThat(noRoles().isMadhyasthaKaryalaya()).isFalse();
    }

    // --- City scope: owns Zones (ADR-0024) ---

    @Test
    void regionalTeamMemberHoldsTheirOwnCityScope() {
        assertThat(rows().regionalTeamIn(CITY, YUVAK).build().isRegionalTeamMemberOfCity(CITY)).isTrue();
    }

    @Test
    void regionalTeamMemberDoesNotHoldAnotherCityScope() {
        assertThat(rows().regionalTeamIn(CITY, YUVAK).build()
                .isRegionalTeamMemberOfCity(OTHER_CITY)).isFalse();
    }

    @Test
    void stateScopeDoesNotConferCityScope() {
        // Zone authority moved MK -> Regional Team (ADR-0024); MK has no City scope.
        assertThat(rows().mk().build().isRegionalTeamMemberOfCity(CITY)).isFalse();
    }

    @Test
    void anOutsiderHoldsNoCityScope() {
        assertThat(noRoles().isRegionalTeamMemberOfCity(CITY)).isFalse();
    }

    @Test
    void cityScopeIgnoresTheDemographicOnTheRow() {
        // ADR-0024: a Zone is geography, so *any* Regional Team row in the City
        // authorizes — this is the asymmetry with holdsRegionalTeamIn below, and it
        // is deliberate.
        CallerAuthority caller = rows().regionalTeamIn(CITY, BAAL).build();

        assertThat(caller.isRegionalTeamMemberOfCity(CITY)).isTrue();
        assertThat(caller.holdsRegionalTeamIn(CITY, YUVAK)).isFalse();
    }

    // --- Zone scope: owns Kshetras (ADR-0009) ---

    @Test
    void sanyojakHoldsTheirOwnZoneScope() {
        assertThat(rows().sanyojakOf(ZONE).build().isSanyojakOfZone(ZONE)).isTrue();
    }

    @Test
    void sanyojakDoesNotHoldAnotherZoneScope() {
        assertThat(rows().sanyojakOf(ZONE).build().isSanyojakOfZone(OTHER_ZONE)).isFalse();
    }

    @Test
    void anOutsiderHoldsNoZoneScope() {
        assertThat(noRoles().isSanyojakOfZone(ZONE)).isFalse();
    }

    @Test
    void stateScopeAloneDoesNotConferZoneScope() {
        // Kshetra authority is the Zone's Sanyojak — MK is a separate tier (ADR-0009).
        assertThat(rows().mk().build().isSanyojakOfZone(ZONE)).isFalse();
    }

    // --- Kshetra scope: owns Sabhas (ADR-0011, ADR-0026) ---

    @Test
    void nirdeshakHoldsTheirOwnKshetraScope() {
        assertThat(rows().nirdeshakIn(KSHETRA, YUVAK).build()
                .holdsNirdeshakIn(KSHETRA, YUVAK)).isTrue();
    }

    @Test
    void nirdeshakOfAnotherDemographicDoesNotHoldThisKshetraScope() {
        assertThat(rows().nirdeshakIn(KSHETRA, YUVAK).build()
                .holdsNirdeshakIn(KSHETRA, BAAL)).isFalse();
    }

    @Test
    void anOutsiderHoldsNoKshetraScope() {
        assertThat(noRoles().holdsNirdeshakIn(KSHETRA, YUVAK)).isFalse();
    }

    @Test
    void sahNirdeshakDoesNotHoldNirdeshakAuthority() {
        // The reopen tiers overlap, the appointing tier does not (ADR-0011).
        assertThat(rows().row("SAH_NIRDESHAK", null, KSHETRA, null, null, YUVAK).build()
                .holdsNirdeshakIn(KSHETRA, YUVAK)).isFalse();
    }

    // --- Running a Sabha ---

    @Test
    void bothSanchalakAndSahSanchalakRunTheSabha() {
        assertThat(rows().row("SANCHALAK", SABHA, null, null, null, null).build()
                .runsSabha(SABHA)).isTrue();
        assertThat(rows().row("SAH_SANCHALAK", SABHA, null, null, null, null).build()
                .runsSabha(SABHA)).isTrue();
    }

    @Test
    void runningOneSabhaSaysNothingAboutAnother() {
        assertThat(rows().row("SANCHALAK", SABHA, null, null, null, null).build()
                .runsSabha(UUID.randomUUID())).isFalse();
    }

    @Test
    void aNirikshakDoesNotRunTheSabhaTheyCover() {
        // The proxy is a different relation entirely (nirikshak_sabha_assignments),
        // and issue #66 split NIRIKSHAK from NIRIKSHAK_PROXY to keep it that way.
        assertThat(rows().row("NIRIKSHAK", SABHA, null, null, null, null).build()
                .runsSabha(SABHA)).isFalse();
    }

    // --- The geographic reads, and the pairs that differ on purpose ---

    @Test
    void oversightKshetrasIncludeSahNirdeshakAndDropTheDemographic() {
        // ADR-0023 makes the audit read broader than write authority, which is why
        // this is not nirdeshakScopes().
        CallerAuthority caller = rows()
                .nirdeshakIn(KSHETRA, YUVAK)
                .row("SAH_NIRDESHAK", null, OTHER_KSHETRA, null, null, BAAL)
                .build();

        assertThat(caller.kshetrasUnderOversight()).containsExactlyInAnyOrder(KSHETRA, OTHER_KSHETRA);
        assertThat(caller.nirdeshakScopes()).containsExactly(new NirdeshakScope(KSHETRA, YUVAK));
    }

    @Test
    void repeatedRegionalTeamRowsInOneCityCollapseToOneEntry() {
        // Membership is per (City, demographic); the geographic read is per City.
        CallerAuthority caller = rows()
                .regionalTeamIn(CITY, YUVAK)
                .regionalTeamIn(CITY, BAAL)
                .build();

        assertThat(caller.regionalTeamCities()).containsExactly(CITY);
    }

    // --- The rows themselves stay in ---

    @Test
    void aRoleOutsideTheOperationalEnumIsSkippedRatherThanFailing() {
        // Sant and MK are OversightRoles, not Roles; the tolerant parse is written
        // once, here, instead of in each adapter.
        CallerAuthority caller = rows().mk().sant()
                .row("SANCHALAK", SABHA, null, null, null, null)
                .build();

        assertThat(caller.operationalRoles()).containsExactly(Role.SANCHALAK);
        assertThat(caller.isMadhyasthaKaryalaya()).isTrue();
        assertThat(caller.isSant()).isTrue();
    }

    @Test
    void theRowsAreCopiedSoALaterMutationCannotChangeAnAnswer() {
        List<RoleAssignment> mutable = new ArrayList<>();
        mutable.add(new RoleAssignment("SANCHALAK", SABHA, null, null, null, null));
        CallerAuthority caller = new CallerAuthority(UserId.of(USER), mutable);

        mutable.clear();

        assertThat(caller.runsSabha(SABHA)).isTrue();
    }

    private static final UUID OTHER_KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000d2");

    private static CallerAuthority noRoles() {
        return CallerAuthority.withNoRoles(UserId.of(USER));
    }

    private static Rows rows() {
        return new Rows();
    }

    /** Literal {@code role_assignments} rows — no fake, because there is no port left. */
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
            return new CallerAuthority(UserId.of(USER), rows);
        }
    }
}
