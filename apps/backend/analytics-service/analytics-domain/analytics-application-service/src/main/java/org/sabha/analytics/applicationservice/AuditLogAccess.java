package org.sabha.analytics.applicationservice;

import java.util.Set;

import org.sabha.common.CallerAuthority;
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
 * them: they simply hold no Nirdeshak-and-above geographic row.</p>
 *
 * <p><b>Portless, and deliberately not deleted.</b> ADR-0032 deleted two engines
 * for having nothing left once the fact layer moved to a parameter; this one
 * ends the fold with zero collaborators and stays, because that rule requires
 * zero ports <em>and</em> zero policy. The tier fold above and the {@code empty ⇒
 * Denied} rule (ADR-0023) are real policy. A reader applying the deletion rule
 * mechanically would get this wrong, which is why it is written down here as
 * well as in the ADR. It keeps its {@code @Service} bean rather than becoming
 * static, so it stays inside the constructor-injection convention and inside
 * rule R3's reviewed list.</p>
 *
 * <p>The geographic sets come from the caller's own {@code role_assignments}
 * rows, which the request edge already loaded — the three {@code SELECT DISTINCT}s
 * of the deleted {@code JdbcAuditScopeLookup} are now three filters over rows in
 * hand. The audit read is deliberately not demographic-filtered, which is why it
 * reads {@link CallerAuthority#kshetrasUnderOversight()} rather than the
 * appointment-authority pair beside it.</p>
 */
@Service
public class AuditLogAccess {

    public AuditScope scopeFor(CallerAuthority caller) {
        if (caller.isMadhyasthaKaryalaya() || caller.isSant()) {
            return new AuditScope.Unrestricted();
        }
        AuditScope.Scoped scoped = new AuditScope.Scoped(
                caller.kshetrasUnderOversight(),
                Set.copyOf(caller.sanyojakZones()),
                Set.copyOf(caller.regionalTeamCities()));
        return scoped.isEmpty() ? new AuditScope.Denied() : scoped;
    }
}
