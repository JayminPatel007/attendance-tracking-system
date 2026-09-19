package org.sabha.common;

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
 *
 * <p><b>Why this one stayed a port when nine others folded.</b> After ADR-0032
 * it is fully caller-keyed and issues zero queries — on the face of it the most
 * foldable thing left in the population. It stays because what it fronts is a
 * <em>policy</em>, not a table, and ADR-0032's whole thesis is that facts
 * consolidate and policies do not. The boolean return is compile-enforced rather
 * than chosen: handing back an {@code AuditScope} would drag an analytics type
 * into {@code identity-domain-core} and break ADR-0019's ring order.</p>
 */
public interface AuditReadAccess {

    boolean canRead(CallerAuthority caller);
}
