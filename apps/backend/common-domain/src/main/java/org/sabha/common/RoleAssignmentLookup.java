package org.sabha.common;

import java.util.Set;
import java.util.UUID;

/**
 * Cross-context port (ADR-0019): resolves the operational roles a user holds on
 * a specific Sabha. The {@code role_assignments} table is owned by the identity
 * bounded context, so the implementation lives in {@code identity-data-access};
 * the port lives in common-domain so the attendance context's Authorization
 * Engine can consult it without crossing the bounded-context seam.
 */
public interface RoleAssignmentLookup {

    Set<Role> rolesForUserOnSabha(UUID userId, UUID sabhaId);
}
