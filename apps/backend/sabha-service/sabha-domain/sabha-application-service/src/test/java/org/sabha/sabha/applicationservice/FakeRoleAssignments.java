package org.sabha.sabha.applicationservice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;

/**
 * Shared test double for {@link RoleAssignmentLookup}: holds the Kshetra-scoped
 * roles a user has, keyed by {@code (user, kshetra, demographic)}. Defaults to no
 * roles, so tests that do not exercise Kshetra-scope authority can pass it
 * un-granted; {@link #grant} adds the rows the authority paths look for.
 */
final class FakeRoleAssignments implements RoleAssignmentLookup {

    private final Map<String, Set<Role>> kshetraRoles = new HashMap<>();

    void grant(UUID userId, UUID kshetraId, String demographic, Role role) {
        kshetraRoles.computeIfAbsent(key(userId, kshetraId, demographic), k -> new HashSet<>()).add(role);
    }

    @Override
    public Set<Role> rolesForUserOnSabha(UUID userId, UUID sabhaId) {
        return Set.of();
    }

    @Override
    public Set<Role> rolesForUserOnKshetra(UUID userId, UUID kshetraId, String demographic) {
        return kshetraRoles.getOrDefault(key(userId, kshetraId, demographic), Set.of());
    }

    private static String key(UUID userId, UUID kshetraId, String demographic) {
        return userId + "|" + kshetraId + "|" + demographic;
    }
}
