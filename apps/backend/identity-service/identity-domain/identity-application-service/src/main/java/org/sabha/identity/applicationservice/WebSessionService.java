package org.sabha.identity.applicationservice;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.sabha.common.Role;
import org.sabha.common.SantLookup;
import org.sabha.identity.domain.VisibleSections;
import org.springframework.stereotype.Service;

/**
 * Resolves an authenticated web (BFF) session to the shell view-model: the local
 * User behind the Keycloak subject, their MK membership, and their visible
 * {@link org.sabha.identity.domain.Section}s (ADR-0022, Slice 9 role-based nav).
 */
@Service
public class WebSessionService {

    private final UserRepository users;
    private final MadhyasthaKaryalayaMembership membership;
    private final SantLookup sant;
    private final UserRolesLookup roles;

    public WebSessionService(
            UserRepository users,
            MadhyasthaKaryalayaMembership membership,
            SantLookup sant,
            UserRolesLookup roles) {
        this.users = users;
        this.membership = membership;
        this.sant = sant;
        this.roles = roles;
    }

    public Optional<WebSession> describe(UUID keycloakSubject) {
        return users.findByKeycloakUserId(keycloakSubject).map(user -> {
            boolean isMk = membership.isMember(user.id());
            boolean isSant = sant.isSant(user.id());
            Set<Role> operationalRoles = roles.operationalRolesOf(user.id());
            return new WebSession(user.username(), isMk, VisibleSections.forMember(isMk, isSant, operationalRoles));
        });
    }
}
