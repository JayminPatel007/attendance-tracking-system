package org.sabha.attendance.applicationservice;

import java.util.Set;
import java.util.UUID;

import org.sabha.common.AuthorizedAction;
import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;
import org.sabha.common.StructuralHierarchyLookup;
import org.springframework.stereotype.Service;

/**
 * Enforces the Sabha-shaping vs day-of-marking vs reopen permission split
 * (ADR-0001).
 *
 * <p>Sabha-shaping actions (cancel, reschedule, venue-override, schedule-change)
 * are restricted to the Sanchalak of the target Sabha; the Sah-Sanchalak is
 * explicitly excluded. The Nirikshak-proxy authority described in ADR-0001 is
 * deliberately <em>not</em> implemented here — it lands in Slice 14.</p>
 *
 * <p>{@link AuthorizedAction#REOPEN} is the higher-tier correction path: only the
 * Kshetra tiers (Nirikshak, Nirdeshak, Sah-Nirdeshak) over the Sabha's own
 * {@code (kshetra, demographic)} may reopen a Finalized Occurrence — never the
 * Sanchalak/Sah-Sanchalak, nor the oversight tiers (Sanyojak, Sant, MK). Those
 * tiers are appointed against a Kshetra and demographic rather than a single
 * Sabha (ADR-0011), so reopen resolves the Sabha to its scope via {@link
 * StructuralHierarchyLookup} and checks the Kshetra-scoped roles.</p>
 *
 * <p>The {@code target} is the Sabha the action acts upon (for an Occurrence,
 * its {@code sabhaId}). Role resolution is delegated to the cross-context
 * {@link RoleAssignmentLookup} port.</p>
 */
@Service
public class AuthorizationEngine {

    private final RoleAssignmentLookup roleAssignments;
    private final StructuralHierarchyLookup hierarchy;

    public AuthorizationEngine(RoleAssignmentLookup roleAssignments, StructuralHierarchyLookup hierarchy) {
        this.roleAssignments = roleAssignments;
        this.hierarchy = hierarchy;
    }

    public boolean canUserDo(UUID userId, AuthorizedAction action, UUID target) {
        if (action == AuthorizedAction.REOPEN) {
            return hierarchy.sabhaScope(target)
                    .map(scope -> roleAssignments
                            .rolesForUserOnKshetra(userId, scope.kshetraId(), scope.demographic())
                            .stream().anyMatch(Role.REOPEN_TIERS::contains))
                    .orElse(false);
        }
        Set<Role> roles = roleAssignments.rolesForUserOnSabha(userId, target);
        if (AuthorizedAction.SABHA_SHAPING_ACTIONS.contains(action)) {
            return roles.contains(Role.SANCHALAK);
        }
        return false;
    }
}
