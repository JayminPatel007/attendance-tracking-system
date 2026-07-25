package org.sabha.attendance.applicationservice;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.sabha.common.AuthorizedAction;
import org.sabha.common.NirikshakAssignmentLookup;
import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;
import org.sabha.common.StructuralHierarchyLookup;
import org.springframework.stereotype.Service;

/**
 * Enforces the Sabha-shaping vs day-of-marking vs reopen permission split
 * (ADR-0001).
 *
 * <p>Sabha-shaping actions (cancel, reschedule, venue-override, schedule-change)
 * are granted to the Sanchalak of the target Sabha; the Sah-Sanchalak is
 * explicitly excluded. They are <em>also</em> granted to a Nirikshak currently
 * assigned to that Sabha, who exercises the full Sanchalak operational toolkit as
 * a proxy when the Sanchalak is unavailable (CONTEXT.md, ADR-0001, Slice 14). The
 * proxy is scoped to the explicit Sabha assignment via {@link
 * NirikshakAssignmentLookup}, so a Nirikshak is rejected on any Sabha outside
 * their assignment. The audit attribution of a proxy action (acting Nirikshak vs
 * on-behalf-of Sanchalak) is the {@link OccurrenceWriter}'s concern,
 * not the engine's.</p>
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
    private final NirikshakAssignmentLookup nirikshakAssignments;

    public AuthorizationEngine(
            RoleAssignmentLookup roleAssignments,
            StructuralHierarchyLookup hierarchy,
            NirikshakAssignmentLookup nirikshakAssignments) {
        this.roleAssignments = roleAssignments;
        this.hierarchy = hierarchy;
        this.nirikshakAssignments = nirikshakAssignments;
    }

    public boolean canUserDo(UUID userId, AuthorizedAction action, UUID target) {
        if (action == AuthorizedAction.REOPEN) {
            return hierarchy.sabhaScope(target)
                    .map(scope -> roleAssignments
                            .rolesForUserOnKshetra(userId, scope.kshetraId(), scope.demographic())
                            .stream().anyMatch(Role.REOPEN_TIERS::contains))
                    .orElse(false);
        }
        if (AuthorizedAction.SABHA_SHAPING_ACTIONS.contains(action)) {
            Set<Role> roles = roleAssignments.rolesForUserOnSabha(userId, target);
            return roles.contains(Role.SANCHALAK)
                    || nirikshakAssignments.isAssignedTo(userId, target);
        }
        return false;
    }

    /**
     * The absent Sanchalak a proxy action is performed on behalf of, for audit
     * attribution (Slice 14). Returns the Sabha's Sanchalak only when {@code
     * userId} is exercising the Nirikshak proxy on a Sabha-shaping action — i.e.
     * the action is shaping, the caller is an assigned Nirikshak, and the caller
     * is not themselves the Sanchalak. Returns empty for a Sanchalak acting on
     * their own Sabha and for non-proxy authorities (e.g. a Nirdeshak reopen).
     */
    public Optional<UUID> onBehalfOf(UUID userId, AuthorizedAction action, UUID target) {
        if (!AuthorizedAction.SABHA_SHAPING_ACTIONS.contains(action)
                || !nirikshakAssignments.isAssignedTo(userId, target)) {
            return Optional.empty();
        }
        return roleAssignments.sanchalakOf(target).filter(sanchalak -> !sanchalak.equals(userId));
    }
}
