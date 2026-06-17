package org.sabha.analytics.applicationservice;

import java.util.UUID;

import org.sabha.common.MadhyasthaKaryalayaLookup;
import org.sabha.common.SantLookup;
import org.springframework.stereotype.Service;

/**
 * The audit-log Authorization Engine (ADR-0023, Slice 19). Maps a caller to the
 * {@link AuditScope} they read: a Sant or Madhyastha Karyalaya member reads the
 * whole State ({@link AuditScope.Unrestricted}); a Nirdeshak / Sah-Nirdeshak,
 * Sanyojak, or Regional Team member reads their resolved geographic scope
 * ({@link AuditScope.Scoped}); everyone below Nirdeshak — and any caller who
 * resolves to no scope at all — is {@link AuditScope.Denied}.
 *
 * <p>The "no scope ⇒ Denied" fold means the forbidden tiers (Sanchalak,
 * Sah-Sanchalak, Nirikshak) are rejected without the engine having to enumerate
 * them: they simply hold no Nirdeshak-and-above geographic row. Stateless apart
 * from its ports, so the rule is exercised end-to-end without a database,
 * mirroring {@link DashboardAccess}.</p>
 */
@Service
public class AuditLogAccess {

    private final MadhyasthaKaryalayaLookup madhyasthaKaryalaya;
    private final SantLookup sants;
    private final AuditScopeLookup scopes;

    public AuditLogAccess(MadhyasthaKaryalayaLookup madhyasthaKaryalaya,
                          SantLookup sants,
                          AuditScopeLookup scopes) {
        this.madhyasthaKaryalaya = madhyasthaKaryalaya;
        this.sants = sants;
        this.scopes = scopes;
    }

    public AuditScope scopeFor(UUID userId) {
        if (madhyasthaKaryalaya.isMember(userId) || sants.isSant(userId)) {
            return new AuditScope.Unrestricted();
        }
        AuditScope.Scoped scoped = scopes.scopeOf(userId);
        return scoped.isEmpty() ? new AuditScope.Denied() : scoped;
    }
}
