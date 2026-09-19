package org.sabha.common;

import java.util.UUID;

/**
 * Cross-context port (ADR-0019): whether a Nirikshak is assigned to cover a
 * given Sabha. Unlike the Kshetra-tier reopen authority — stored against {@code
 * (kshetra, demographic)} in {@code role_assignments} and read from {@link
 * CallerAuthority#rolesOnKshetra(UUID, String)} — the Nirikshak's
 * Sanchalak-proxy capability is scoped to the explicit set of 3–4 Sabhas a
 * Nirdeshak has assigned to them (CONTEXT.md, Slice 14).
 *
 * <p>It survived the ADR-0032 fold on purpose, and it is the carve-out most
 * likely to be questioned: post-fold it is fully caller-keyed. It stays because
 * it is a <b>different relation</b> with a different lifecycle — {@code
 * nirikshak_sabha_assignments}, hard-deleted, with no {@code revoked_at} column —
 * and because issue #66 split {@code NIRIKSHAK} from {@code NIRIKSHAK_PROXY}
 * precisely to keep the proxy set distinct from the role row. Folding it would
 * re-make that conflation in the name of tidiness and charge every request a
 * second query for a fact two call sites use.</p>
 *
 * <p>{@code sabhasAssignedTo} was deleted with the fold: it had zero production
 * consumers. The assignment is owned by the identity bounded context, so the
 * implementation lives in {@code identity-data-access}.</p>
 */
public interface NirikshakAssignmentLookup {

    /** Whether {@code userId} is a Nirikshak currently assigned to {@code sabhaId}. */
    boolean isAssignedTo(UUID userId, UUID sabhaId);
}
