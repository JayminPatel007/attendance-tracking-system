package org.sabha.identity.applicationservice.appointment;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Read/write port over {@code role_assignments} for revocation (ADR-0026).
 * Revocation is a state change, so the single write is {@link #markRevoked}; the
 * row and its {@code appointedBy} reference survive. The reads serve the two
 * guards {@link RoleRevocationService} applies around the write: the Regional
 * Team last-one-out rule (ADR-0025 §2) and the loss of login on a User's last
 * active role (ADR-0026). All reads count only active (non-revoked) rows.
 */
public interface RevokableRoleAssignments {

    /** The active assignment with this id, or empty if it is unknown or already revoked. */
    Optional<RevokableAssignment> findActive(UUID assignmentId);

    void markRevoked(UUID assignmentId, UUID revokedBy, Instant revokedAt);

    /** Active Regional Team members of a (City, demographic), for the last-one-out guard. */
    int activeRegionalTeamCount(UUID cityId, String demographic);

    /** How many active role assignments the User still holds across all scopes. */
    int activeRoleCountForUser(UUID userId);
}
