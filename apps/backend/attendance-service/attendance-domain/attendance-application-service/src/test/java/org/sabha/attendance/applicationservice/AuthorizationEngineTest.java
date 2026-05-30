package org.sabha.attendance.applicationservice;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.AuthorizedAction;
import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationEngineTest {

    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SANCHALAK = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID SAH_SANCHALAK = UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Test
    void sanchalakIsAllowedEverySabhaShapingAction() {
        AuthorizationEngine engine = new AuthorizationEngine(fixedRoles(Map.of(
                key(SANCHALAK, SABHA_ID), Set.of(Role.SANCHALAK))));

        for (AuthorizedAction action : AuthorizedAction.SABHA_SHAPING_ACTIONS) {
            assertThat(engine.canUserDo(SANCHALAK, action, SABHA_ID))
                    .as("Sanchalak should be allowed to %s", action)
                    .isTrue();
        }
    }

    @Test
    void sahSanchalakIsDeniedEverySabhaShapingAction() {
        AuthorizationEngine engine = new AuthorizationEngine(fixedRoles(Map.of(
                key(SAH_SANCHALAK, SABHA_ID), Set.of(Role.SAH_SANCHALAK))));

        for (AuthorizedAction action : AuthorizedAction.SABHA_SHAPING_ACTIONS) {
            assertThat(engine.canUserDo(SAH_SANCHALAK, action, SABHA_ID))
                    .as("Sah-Sanchalak should be denied %s", action)
                    .isFalse();
        }
    }

    @Test
    void aUserWithNoRoleOnTheSabhaIsDenied() {
        AuthorizationEngine engine = new AuthorizationEngine(fixedRoles(Map.of()));

        assertThat(engine.canUserDo(SANCHALAK, AuthorizedAction.CANCEL, SABHA_ID)).isFalse();
    }

    private static String key(UUID userId, UUID sabhaId) {
        return userId + ":" + sabhaId;
    }

    private static RoleAssignmentLookup fixedRoles(Map<String, Set<Role>> roles) {
        return (userId, sabhaId) -> roles.getOrDefault(key(userId, sabhaId), Set.of());
    }
}
