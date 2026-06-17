package org.sabha.identity.applicationservice.session;

import java.util.Set;
import java.util.UUID;

import org.sabha.common.Role;

/**
 * Driven port returning every operational {@link Role} a user holds across all
 * scopes (any Sabha / Kshetra). Used to decide web shell section visibility,
 * which is not Sabha-scoped — unlike {@code RoleAssignmentLookup}. Non-operational
 * assignments (e.g. {@code MADHYASTHA_KARYALAYA}) are not {@link Role}s and are
 * excluded. Adapter in {@code identity-data-access}.
 */
public interface UserRolesLookup {

    Set<Role> operationalRolesOf(UUID userId);
}
