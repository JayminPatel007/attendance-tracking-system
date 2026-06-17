package org.sabha.identity.applicationservice;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.sabha.common.AuditReadAccess;
import org.sabha.common.Role;
import org.sabha.identity.domain.VisibleSections;
import org.springframework.stereotype.Service;

/**
 * Resolves an authenticated web (BFF) session to the shell view-model: the local
 * User behind the Keycloak subject, their MK membership, and their visible
 * {@link org.sabha.identity.domain.Section}s (ADR-0022, Slice 9 role-based nav).
 *
 * <p>The Audit-log section is gated by {@link AuditReadAccess}, the same
 * scope-resolution the audit BFF uses, rather than a tier rule restated here — so
 * the sidebar admits exactly the set the engine admits, the Regional Team
 * included (issue #80).</p>
 */
@Service
public class WebSessionService {

    private final UserRepository users;
    private final MadhyasthaKaryalayaMembership membership;
    private final AuditReadAccess auditReadAccess;
    private final UserRolesLookup roles;

    public WebSessionService(
            UserRepository users,
            MadhyasthaKaryalayaMembership membership,
            AuditReadAccess auditReadAccess,
            UserRolesLookup roles) {
        this.users = users;
        this.membership = membership;
        this.auditReadAccess = auditReadAccess;
        this.roles = roles;
    }

    public Optional<WebSession> describe(UUID keycloakSubject) {
        return users.findByKeycloakUserId(keycloakSubject).map(user -> {
            boolean isMk = membership.isMember(user.id());
            boolean canReadAudit = auditReadAccess.canRead(user.id());
            Set<Role> operationalRoles = roles.operationalRolesOf(user.id());
            return new WebSession(user.username(), isMk, VisibleSections.forMember(isMk, canReadAudit, operationalRoles));
        });
    }
}
