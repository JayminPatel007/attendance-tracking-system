package org.sabha.attendance.applicationservice;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.AuthorizedAction;
import org.sabha.common.NirikshakAssignmentLookup;
import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;
import org.sabha.common.SabhaScope;
import org.sabha.common.StructuralHierarchyLookup;

import static org.assertj.core.api.Assertions.assertThat;

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
        AuthorizationEngine engine = engine(sabhaRoles(Map.of(
                key(SANCHALAK, SABHA_ID), Set.of(Role.SANCHALAK))), kshetraRoles(Map.of()));

        for (AuthorizedAction action : AuthorizedAction.SABHA_SHAPING_ACTIONS) {
            assertThat(engine.canUserDo(SANCHALAK, action, SABHA_ID))
                    .as("Sanchalak should be allowed to %s", action)
                    .isTrue();
        }
    }

    @Test
    void sahSanchalakIsDeniedEverySabhaShapingAction() {
        AuthorizationEngine engine = engine(sabhaRoles(Map.of(
                key(SAH_SANCHALAK, SABHA_ID), Set.of(Role.SAH_SANCHALAK))), kshetraRoles(Map.of()));

        for (AuthorizedAction action : AuthorizedAction.SABHA_SHAPING_ACTIONS) {
            assertThat(engine.canUserDo(SAH_SANCHALAK, action, SABHA_ID))
                    .as("Sah-Sanchalak should be denied %s", action)
                    .isFalse();
        }
    }

    @Test
    void aUserWithNoRoleOnTheSabhaIsDenied() {
        AuthorizationEngine engine = engine(sabhaRoles(Map.of()), kshetraRoles(Map.of()));

        assertThat(engine.canUserDo(SANCHALAK, AuthorizedAction.CANCEL, SABHA_ID)).isFalse();
    }

    @Test
    void aNirikshakAssignedToTheSabhaMayProxyEverySabhaShapingAction() {
        AuthorizationEngine engine = engine(
                sabhaRoles(Map.of()), kshetraRoles(Map.of()),
                assignments(Map.of(NIRIKSHAK, Set.of(SABHA_ID))));

        for (AuthorizedAction action : AuthorizedAction.SABHA_SHAPING_ACTIONS) {
            assertThat(engine.canUserDo(NIRIKSHAK, action, SABHA_ID))
                    .as("Nirikshak assigned to the Sabha should be allowed to proxy %s", action)
                    .isTrue();
        }
    }

    @Test
    void aNirikshakNotAssignedToTheSabhaIsDeniedSabhaShapingActions() {
        UUID otherSabha = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
        AuthorizationEngine engine = engine(
                sabhaRoles(Map.of()), kshetraRoles(Map.of()),
                assignments(Map.of(NIRIKSHAK, Set.of(otherSabha))));

        for (AuthorizedAction action : AuthorizedAction.SABHA_SHAPING_ACTIONS) {
            assertThat(engine.canUserDo(NIRIKSHAK, action, SABHA_ID))
                    .as("Nirikshak not assigned to the Sabha should be denied %s", action)
                    .isFalse();
        }
    }

    @Test
    void nirikshakNirdeshakAndSahNirdeshakOnTheSabhasKshetraMayReopen() {
        AuthorizationEngine engine = engine(sabhaRoles(Map.of()), kshetraRoles(Map.of(
                kshetraKey(NIRIKSHAK, KSHETRA_ID, DEMOGRAPHIC), Set.of(Role.NIRIKSHAK),
                kshetraKey(NIRDESHAK, KSHETRA_ID, DEMOGRAPHIC), Set.of(Role.NIRDESHAK),
                kshetraKey(SAH_NIRDESHAK, KSHETRA_ID, DEMOGRAPHIC), Set.of(Role.SAH_NIRDESHAK))));

        assertThat(engine.canUserDo(NIRIKSHAK, AuthorizedAction.REOPEN, SABHA_ID)).isTrue();
        assertThat(engine.canUserDo(NIRDESHAK, AuthorizedAction.REOPEN, SABHA_ID)).isTrue();
        assertThat(engine.canUserDo(SAH_NIRDESHAK, AuthorizedAction.REOPEN, SABHA_ID)).isTrue();
    }

    @Test
    void sanchalakSahSanchalakAndSanyojakMayNotReopen() {
        AuthorizationEngine engine = engine(
                sabhaRoles(Map.of(key(SANCHALAK, SABHA_ID), Set.of(Role.SANCHALAK))),
                kshetraRoles(Map.of(
                        kshetraKey(SAH_SANCHALAK, KSHETRA_ID, DEMOGRAPHIC), Set.of(Role.SAH_SANCHALAK),
                        kshetraKey(SANYOJAK, KSHETRA_ID, DEMOGRAPHIC), Set.of(Role.SANYOJAK))));

        assertThat(engine.canUserDo(SANCHALAK, AuthorizedAction.REOPEN, SABHA_ID)).isFalse();
        assertThat(engine.canUserDo(SAH_SANCHALAK, AuthorizedAction.REOPEN, SABHA_ID)).isFalse();
        assertThat(engine.canUserDo(SANYOJAK, AuthorizedAction.REOPEN, SABHA_ID)).isFalse();
    }

    @Test
    void aNirdeshakForADifferentDemographicMayNotReopen() {
        AuthorizationEngine engine = engine(sabhaRoles(Map.of()), kshetraRoles(Map.of(
                kshetraKey(NIRDESHAK, KSHETRA_ID, "YUVATI"), Set.of(Role.NIRDESHAK))));

        assertThat(engine.canUserDo(NIRDESHAK, AuthorizedAction.REOPEN, SABHA_ID)).isFalse();
    }

    @Test
    void aNirdeshakOnADifferentKshetraMayNotReopen() {
        UUID otherKshetra = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        AuthorizationEngine engine = engine(sabhaRoles(Map.of()), kshetraRoles(Map.of(
                kshetraKey(NIRDESHAK, otherKshetra, DEMOGRAPHIC), Set.of(Role.NIRDESHAK))));

        assertThat(engine.canUserDo(NIRDESHAK, AuthorizedAction.REOPEN, SABHA_ID)).isFalse();
    }

    private static String key(UUID userId, UUID sabhaId) {
        return userId + ":" + sabhaId;
    }

    private static String kshetraKey(UUID userId, UUID kshetraId, String demographic) {
        return userId + ":" + kshetraId + ":" + demographic;
    }

    private static RoleAssignmentLookup sabhaRoles(Map<String, Set<Role>> sabha) {
        return new FixedRoleAssignments(sabha, Map.of());
    }

    private static FixedRoleAssignments kshetraRoles(Map<String, Set<Role>> kshetra) {
        return new FixedRoleAssignments(Map.of(), kshetra);
    }

    private static NirikshakAssignmentLookup assignments(Map<UUID, Set<UUID>> bySabha) {
        return new NirikshakAssignmentLookup() {
            @Override
            public boolean isAssignedTo(UUID userId, UUID sabhaId) {
                return bySabha.getOrDefault(userId, Set.of()).contains(sabhaId);
            }

            @Override
            public Set<UUID> sabhasAssignedTo(UUID userId) {
                return bySabha.getOrDefault(userId, Set.of());
            }
        };
    }

    private static AuthorizationEngine engine(RoleAssignmentLookup sabha, FixedRoleAssignments kshetra) {
        return engine(sabha, kshetra, assignments(Map.of()));
    }

    private static AuthorizationEngine engine(
            RoleAssignmentLookup sabha, FixedRoleAssignments kshetra, NirikshakAssignmentLookup assignments) {
        // The Sabha sits in KSHETRA_ID with demographic DEMOGRAPHIC.
        StructuralHierarchyLookup hierarchy = new StructuralHierarchyLookup() {
            @Override
            public Optional<SabhaScope> sabhaScope(UUID sabhaId) {
                return sabhaId.equals(SABHA_ID)
                        ? Optional.of(new SabhaScope(KSHETRA_ID, DEMOGRAPHIC, "REGULAR"))
                        : Optional.empty();
            }

            @Override
            public Optional<UUID> zoneOfKshetra(UUID kshetraId) {
                return Optional.empty();
            }

            @Override
            public Optional<UUID> cityOfZone(UUID zoneId) {
                return Optional.empty();
            }
        };
        RoleAssignmentLookup roles = new FixedRoleAssignments(
                ((FixedRoleAssignments) sabha).sabha, kshetra.kshetra);
        return new AuthorizationEngine(roles, hierarchy, assignments);
    }

    private record FixedRoleAssignments(Map<String, Set<Role>> sabha, Map<String, Set<Role>> kshetra)
            implements RoleAssignmentLookup {

        @Override
        public Set<Role> rolesForUserOnSabha(UUID userId, UUID sabhaId) {
            return sabha.getOrDefault(userId + ":" + sabhaId, Set.of());
        }

        @Override
        public Set<Role> rolesForUserOnKshetra(UUID userId, UUID kshetraId, String demographic) {
            return kshetra.getOrDefault(userId + ":" + kshetraId + ":" + demographic, Set.of());
        }
    }
}
