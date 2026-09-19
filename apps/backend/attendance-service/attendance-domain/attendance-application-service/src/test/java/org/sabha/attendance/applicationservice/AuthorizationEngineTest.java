package org.sabha.attendance.applicationservice;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.AuthorizedAction;
import org.sabha.common.CallerAuthority;
import org.sabha.common.NirikshakAssignmentLookup;
import org.sabha.common.Role;
import org.sabha.common.RoleAssignment;
import org.sabha.common.SabhaFact;
import org.sabha.common.SabhaFacts;
import org.sabha.common.SabhaScope;
import org.sabha.common.UserId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Sabha-shaping / reopen permission split (ADR-0001).
 *
 * <p>The caller's roles arrive as a parameter since ADR-0032, so the two
 * role-keyed maps this test used to thread through a fake are now rows on the
 * caller. The engine's three remaining ports are all keyed by the <em>target</em>
 * Sabha, which is exactly why the fold left its constructor the size it was.</p>
 */
class AuthorizationEngineTest {

    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID KSHETRA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String DEMOGRAPHIC = "YUVAK";
    private static final UUID SANCHALAK = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID SAH_SANCHALAK = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final UUID NIRIKSHAK = UUID.fromString("00000000-0000-0000-0000-000000000031");
    private static final UUID NIRDESHAK = UUID.fromString("00000000-0000-0000-0000-000000000032");
    private static final UUID SAH_NIRDESHAK = UUID.fromString("00000000-0000-0000-0000-000000000033");
    private static final UUID SANYOJAK = UUID.fromString("00000000-0000-0000-0000-000000000034");

    @Test
    void sanchalakIsAllowedEverySabhaShapingAction() {
        AuthorizationEngine engine = engine();
        CallerAuthority caller = onSabha(SANCHALAK, "SANCHALAK", SABHA_ID);

        for (AuthorizedAction action : AuthorizedAction.SABHA_SHAPING_ACTIONS) {
            assertThat(engine.canUserDo(caller, action, SABHA_ID))
                    .as("Sanchalak should be allowed to %s", action)
                    .isTrue();
        }
    }

    @Test
    void sahSanchalakIsDeniedEverySabhaShapingAction() {
        AuthorizationEngine engine = engine();
        CallerAuthority caller = onSabha(SAH_SANCHALAK, "SAH_SANCHALAK", SABHA_ID);

        for (AuthorizedAction action : AuthorizedAction.SABHA_SHAPING_ACTIONS) {
            assertThat(engine.canUserDo(caller, action, SABHA_ID))
                    .as("Sah-Sanchalak should be denied %s", action)
                    .isFalse();
        }
    }

    @Test
    void aUserWithNoRoleOnTheSabhaIsDenied() {
        AuthorizationEngine engine = engine();

        assertThat(engine.canUserDo(noRoles(SANCHALAK), AuthorizedAction.CANCEL, SABHA_ID)).isFalse();
    }

    @Test
    void aNirikshakAssignedToTheSabhaMayProxyEverySabhaShapingAction() {
        AuthorizationEngine engine = engine(assignments(Map.of(NIRIKSHAK, Set.of(SABHA_ID))));

        for (AuthorizedAction action : AuthorizedAction.SABHA_SHAPING_ACTIONS) {
            assertThat(engine.canUserDo(noRoles(NIRIKSHAK), action, SABHA_ID))
                    .as("Nirikshak assigned to the Sabha should be allowed to proxy %s", action)
                    .isTrue();
        }
    }

    @Test
    void aNirikshakNotAssignedToTheSabhaIsDeniedSabhaShapingActions() {
        UUID otherSabha = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
        AuthorizationEngine engine = engine(assignments(Map.of(NIRIKSHAK, Set.of(otherSabha))));

        for (AuthorizedAction action : AuthorizedAction.SABHA_SHAPING_ACTIONS) {
            assertThat(engine.canUserDo(noRoles(NIRIKSHAK), action, SABHA_ID))
                    .as("Nirikshak not assigned to the Sabha should be denied %s", action)
                    .isFalse();
        }
    }

    @Test
    void nirikshakNirdeshakAndSahNirdeshakOnTheSabhasKshetraMayReopen() {
        AuthorizationEngine engine = engine();

        assertThat(engine.canUserDo(
                onKshetra(NIRIKSHAK, "NIRIKSHAK", KSHETRA_ID, DEMOGRAPHIC),
                AuthorizedAction.REOPEN, SABHA_ID)).isTrue();
        assertThat(engine.canUserDo(
                onKshetra(NIRDESHAK, "NIRDESHAK", KSHETRA_ID, DEMOGRAPHIC),
                AuthorizedAction.REOPEN, SABHA_ID)).isTrue();
        assertThat(engine.canUserDo(
                onKshetra(SAH_NIRDESHAK, "SAH_NIRDESHAK", KSHETRA_ID, DEMOGRAPHIC),
                AuthorizedAction.REOPEN, SABHA_ID)).isTrue();
    }

    @Test
    void sanchalakSahSanchalakAndSanyojakMayNotReopen() {
        AuthorizationEngine engine = engine();

        assertThat(engine.canUserDo(
                onSabha(SANCHALAK, "SANCHALAK", SABHA_ID),
                AuthorizedAction.REOPEN, SABHA_ID)).isFalse();
        assertThat(engine.canUserDo(
                onKshetra(SAH_SANCHALAK, "SAH_SANCHALAK", KSHETRA_ID, DEMOGRAPHIC),
                AuthorizedAction.REOPEN, SABHA_ID)).isFalse();
        assertThat(engine.canUserDo(
                onKshetra(SANYOJAK, "SANYOJAK", KSHETRA_ID, DEMOGRAPHIC),
                AuthorizedAction.REOPEN, SABHA_ID)).isFalse();
    }

    @Test
    void aNirdeshakForADifferentDemographicMayNotReopen() {
        AuthorizationEngine engine = engine();

        assertThat(engine.canUserDo(
                onKshetra(NIRDESHAK, "NIRDESHAK", KSHETRA_ID, "YUVATI"),
                AuthorizedAction.REOPEN, SABHA_ID)).isFalse();
    }

    @Test
    void aNirdeshakOnADifferentKshetraMayNotReopen() {
        UUID otherKshetra = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        AuthorizationEngine engine = engine();

        assertThat(engine.canUserDo(
                onKshetra(NIRDESHAK, "NIRDESHAK", otherKshetra, DEMOGRAPHIC),
                AuthorizedAction.REOPEN, SABHA_ID)).isFalse();
    }

    @Test
    void theProxyAttributesTheActionToTheAbsentSanchalak() {
        AuthorizationEngine engine = engine(assignments(Map.of(NIRIKSHAK, Set.of(SABHA_ID))));

        assertThat(engine.onBehalfOf(noRoles(NIRIKSHAK), AuthorizedAction.CANCEL, SABHA_ID))
                .contains(SANCHALAK);
    }

    @Test
    void aSanchalakActingOnTheirOwnSabhaIsNotActingOnAnyonesBehalf() {
        AuthorizationEngine engine = engine(assignments(Map.of(SANCHALAK, Set.of(SABHA_ID))));

        assertThat(engine.onBehalfOf(
                onSabha(SANCHALAK, "SANCHALAK", SABHA_ID), AuthorizedAction.CANCEL, SABHA_ID))
                .isEmpty();
    }

    private static CallerAuthority noRoles(UUID userId) {
        return CallerAuthority.withNoRoles(UserId.of(userId));
    }

    private static CallerAuthority onSabha(UUID userId, String role, UUID sabhaId) {
        return new CallerAuthority(UserId.of(userId),
                List.of(new RoleAssignment(role, sabhaId, null, null, null, null)));
    }

    private static CallerAuthority onKshetra(UUID userId, String role, UUID kshetraId, String demographic) {
        return new CallerAuthority(UserId.of(userId),
                List.of(new RoleAssignment(role, null, kshetraId, null, null, demographic)));
    }

    private static NirikshakAssignmentLookup assignments(Map<UUID, Set<UUID>> bySabha) {
        return (userId, sabhaId) -> bySabha.getOrDefault(userId, Set.of()).contains(sabhaId);
    }

    private static AuthorizationEngine engine() {
        return engine(assignments(Map.of()));
    }

    private static AuthorizationEngine engine(NirikshakAssignmentLookup assignments) {
        // The Sabha sits in KSHETRA_ID with demographic DEMOGRAPHIC, and SANCHALAK runs it.
        SabhaFacts sabhaFacts = new SabhaFacts() {
            @Override
            public Optional<SabhaFact> of(UUID sabhaId) {
                return sabhaId.equals(SABHA_ID)
                        ? Optional.of(SabhaFact.monthlyAdHoc(
                                sabhaId, new SabhaScope(KSHETRA_ID, DEMOGRAPHIC, "REGULAR"), false))
                        : Optional.empty();
            }

            @Override
            public List<SabhaFact> allWeekly() {
                return List.of();
            }

            @Override
            public Optional<SabhaFact> selectiveIn(UUID kshetraId, String demographic, String track) {
                return Optional.empty();
            }
        };
        return new AuthorizationEngine(
                sabhaId -> sabhaId.equals(SABHA_ID) ? Optional.of(SANCHALAK) : Optional.empty(),
                sabhaFacts, assignments);
    }
}
