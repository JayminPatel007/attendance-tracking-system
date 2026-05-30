package org.sabha.attendance.applicationservice;

import java.util.Set;
import java.util.UUID;

import org.sabha.common.AuthorizedAction;
import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;
import org.springframework.stereotype.Service;

/**
 * Enforces the Sabha-shaping vs day-of-marking permission split (ADR-0001).
 *
 * <p>Sabha-shaping actions (cancel, reschedule, venue-override, schedule-change)
 * are restricted to the Sanchalak of the target Sabha; the Sah-Sanchalak is
 * explicitly excluded. The Nirikshak-proxy authority described in ADR-0001 is
 * deliberately <em>not</em> implemented here — it lands in Slice 14.</p>
 *
 * <p>The {@code target} is the Sabha the action acts upon (for an Occurrence,
 * its {@code sabhaId}). Role resolution is delegated to the cross-context
 * {@link RoleAssignmentLookup} port.</p>
 */
@Service
public class AuthorizationEngine {

    private final RoleAssignmentLookup roleAssignments;

    public AuthorizationEngine(RoleAssignmentLookup roleAssignments) {
        this.roleAssignments = roleAssignments;
    }

    public boolean canUserDo(UUID userId, AuthorizedAction action, UUID target) {
        Set<Role> roles = roleAssignments.rolesForUserOnSabha(userId, target);
        if (AuthorizedAction.SABHA_SHAPING_ACTIONS.contains(action)) {
            return roles.contains(Role.SANCHALAK);
        }
        return false;
    }
}
