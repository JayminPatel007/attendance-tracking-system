package org.sabha.common;

import java.util.Set;
import java.util.UUID;

/**
 * Cross-context port (ADR-0019): resolves which Sabhas a Nirikshak is assigned to
 * cover. Unlike the Kshetra-tier reopen authority (which is stored against {@code
 * (kshetra, demographic)} and read via {@link RoleAssignmentLookup}), the
 * Nirikshak's Sanchalak-proxy capability is scoped to the explicit set of 3–4
 * Sabhas a Nirdeshak has assigned to them (CONTEXT.md, Slice 14). The assignment
 * is mutable and is owned by the identity bounded context, so the implementation
 * lives in {@code identity-data-access}; the port lives in common-domain so the
 * attendance context's Authorization Engine can consult it across the seam.
 */
public interface NirikshakAssignmentLookup {

    /** Whether {@code userId} is a Nirikshak currently assigned to {@code sabhaId}. */
    boolean isAssignedTo(UUID userId, UUID sabhaId);

    /** The Sabhas a Nirikshak is currently assigned to cover (empty if none). */
    Set<UUID> sabhasAssignedTo(UUID userId);
}
