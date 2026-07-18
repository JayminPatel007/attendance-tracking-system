package org.sabha.identity.applicationservice.appointment;

import java.time.Clock;
import java.util.UUID;

import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.AuthorizedAction;
import org.sabha.common.CallerResolver;
import org.sabha.identity.applicationservice.IdentityProviderGateway;
import org.sabha.identity.applicationservice.UserRepository;
import org.sabha.identity.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revokes one role assignment (ADR-0025 §1-2, ADR-0026) in a single transaction:
 *
 * <ol>
 *   <li>Resolve the active assignment — an unknown or already-revoked id is a 404
 *       ({@link RoleAssignmentNotFoundException}).</li>
 *   <li>Authorize the actor by the assignment's <em>scope</em>, not by who made
 *       it: the same {@link AppointmentAuthorization} that gates appointment, so a
 *       successor may revoke a predecessor's assignment. A denial is a 403.</li>
 *   <li>Apply the Regional Team last-one-out guard (ADR-0025 §2): refuse to empty
 *       a (City, demographic) of its final member ({@link LastRegionalTeamMemberException},
 *       409).</li>
 *   <li>Record the revocation as a state change ({@code revokedBy} / {@code
 *       revokedAt}); the row and its {@code appointedBy} survive, and the holder's
 *       appointees and created structures stay attached to the scope — no cascade.</li>
 *   <li>If that was the User's last active role, withdraw their login (ADR-0026);
 *       the Person record persists.</li>
 * </ol>
 */
@Service
public class RoleRevocationService implements RevokeRole {

    private final CallerResolver callerResolver;
    private final AppointmentAuthorization authz;
    private final RevokableRoleAssignments assignments;
    private final UserRepository users;
    private final IdentityProviderGateway identityProvider;
    private final Clock clock;

    public RoleRevocationService(
            CallerResolver callerResolver,
            AppointmentAuthorization authz,
            RevokableRoleAssignments assignments,
            UserRepository users,
            IdentityProviderGateway identityProvider,
            Clock clock) {
        this.callerResolver = callerResolver;
        this.authz = authz;
        this.assignments = assignments;
        this.users = users;
        this.identityProvider = identityProvider;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void revoke(UUID keycloakSubject, UUID assignmentId) {
        UUID actor = callerResolver.requireUserId(keycloakSubject);

        RevokableAssignment assignment = assignments.findActive(assignmentId)
                .orElseThrow(() -> new RoleAssignmentNotFoundException(assignmentId));

        if (!authz.canAppoint(actor, assignment.scope())) {
            throw new AuthorizationDeniedException(actor, AuthorizedAction.REVOKE_ROLE);
        }

        enforceRegionalTeamLastOneOut(assignment.scope());

        assignments.markRevoked(assignmentId, actor, clock.instant());

        withdrawLoginIfLastRole(assignment.userId());
    }

    /**
     * The Regional Team is self-replicating but never empty (ADR-0025 §2): if this
     * is the only active member left for the (City, demographic), the revocation is
     * refused. Checked before the state change so nothing is written on rejection.
     */
    private void enforceRegionalTeamLastOneOut(AppointmentScope scope) {
        if (scope.role() == AppointableRole.REGIONAL_TEAM
                && assignments.activeRegionalTeamCount(scope.cityId(), scope.demographic()) <= 1) {
            throw new LastRegionalTeamMemberException(scope.cityId(), scope.demographic());
        }
    }

    /**
     * A User's login exists to back their roles; once the last active role is gone
     * the account is disabled at the identity provider (ADR-0026). The Person and
     * the {@code role_assignments} history remain.
     */
    private void withdrawLoginIfLastRole(UUID userId) {
        if (assignments.activeRoleCountForUser(userId) == 0) {
            users.findById(userId)
                    .map(User::keycloakUserId)
                    .ifPresent(identityProvider::disableUser);
        }
    }
}
