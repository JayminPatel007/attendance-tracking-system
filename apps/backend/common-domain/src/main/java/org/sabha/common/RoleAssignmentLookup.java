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

    /**
     * The Kshetra-scoped operational roles a user holds for a given {@code
     * (kshetraId, demographic)}. The Kshetra-tier reopen authorities (Nirikshak,
     * Nirdeshak, Sah-Nirdeshak — ADR-0001) are stored against the Kshetra and
     * demographic rather than a single Sabha (ADR-0011 appointment), so the
     * attendance context's Authorization Engine resolves a Sabha to its {@code
     * (kshetra, demographic)} scope and asks this for the matching roles.
     */
    Set<Role> rolesForUserOnKshetra(UUID userId, UUID kshetraId, String demographic);
}
