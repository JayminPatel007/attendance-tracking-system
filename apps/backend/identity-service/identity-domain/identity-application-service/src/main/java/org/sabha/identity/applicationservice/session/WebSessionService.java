package org.sabha.identity.applicationservice.session;

import java.util.Optional;
import java.util.UUID;

import org.sabha.common.AuditReadAccess;
import org.sabha.common.RegionalTeamCityLookup;
import org.sabha.identity.applicationservice.UserRepository;
import org.sabha.identity.applicationservice.bootstrap.MadhyasthaKaryalayaMembership;
import org.sabha.identity.domain.MemberAuthority;
import org.sabha.identity.domain.VisibleSections;
import org.springframework.stereotype.Service;

/**
 * Resolves an authenticated web (BFF) session to the shell view-model: the local
 * User behind the Keycloak subject, their MK and Regional Team membership, and
 * their visible {@link org.sabha.identity.domain.Section}s (ADR-0022, Slice 9
 * role-based nav).
 *
 * <p>Regional Team membership now unlocks Structural Admin for Zone creation
 * (ADR-0024, issue #84), so the session carries it alongside MK; it is resolved
 * through the {@link RegionalTeamCityLookup} City scope, non-empty meaning member.
 * The Audit-log section is gated by {@link AuditReadAccess}, the same
 * scope-resolution the audit BFF uses, rather than a tier rule restated here — so
 * the sidebar admits exactly the set the engine admits, the Regional Team
 * included (issue #80).</p>
 */
@Service
public class WebSessionService {

    private final UserRepository users;
    private final MadhyasthaKaryalayaMembership membership;
    private final RegionalTeamCityLookup regionalTeamCities;
    private final AuditReadAccess auditReadAccess;
    private final UserRolesLookup roles;

    public WebSessionService(
            UserRepository users,
            MadhyasthaKaryalayaMembership membership,
            RegionalTeamCityLookup regionalTeamCities,
            AuditReadAccess auditReadAccess,
            UserRolesLookup roles) {
        this.users = users;
        this.membership = membership;
        this.regionalTeamCities = regionalTeamCities;
        this.auditReadAccess = auditReadAccess;
        this.roles = roles;
    }

    public Optional<WebSession> describe(UUID keycloakSubject) {
        return users.findByKeycloakUserId(keycloakSubject).map(user -> {
            MemberAuthority authority = new MemberAuthority(
                    membership.isMember(user.id()),
                    !regionalTeamCities.citiesOf(user.id()).isEmpty(),
                    auditReadAccess.canRead(user.id()),
                    roles.operationalRolesOf(user.id()));
            return new WebSession(
                    user.username(),
                    authority.madhyasthaKaryalaya(),
                    authority.regionalTeam(),
                    VisibleSections.forMember(authority));
        });
    }
}
