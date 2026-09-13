package org.sabha.common;

import java.util.UUID;

/**
 * Cross-context read port (ADR-0019, ADR-0023): whether a user may read the
 * audit log at all. Lives in common-domain so the identity web shell can decide
 * the Audit-log section's visibility without depending on analytics' domain
 * types and without re-deriving the tier rule — the one authority is the
 * analytics audit Authorization Engine ({@code AuditLogAccess}), whose
 * scope-resolution this port surfaces as a boolean. The implementation lives in
 * analytics-application-service (the context that owns the engine), mirroring the
 * way {@link SantLookup} and {@link MadhyasthaKaryalayaLookup} are owned by
 * identity.
 *
 * <p>Folding the engine's {@code AuditScope} down to "admitted or not" keeps the
 * web nav gate and the BFF in lockstep by construction: there is no separate
 * audit-tier set for the two surfaces to drift apart (issue #80). A caller is
 * admitted exactly when the engine resolves them to a non-{@code Denied} scope —
 * which includes the City-scoped Regional Team, a tier that is deliberately not
 * an operational {@link Role}.</p>
 */
public interface AuditReadAccess {

    boolean canRead(UUID userId);
}
