package org.sabha.analytics.applicationservice;

import java.util.Set;
import java.util.UUID;

/**
 * The slice of the audit feed a caller may read (ADR-0023, Slice 19), decided by
 * {@link AuditLogAccess}.
 *
 * <ul>
 *   <li>{@link Unrestricted} — the State-wide oversight bodies (Sant, with their
 *       universal read, and the Madhyastha Karyalaya). Sees every entry,
 *       including the state-level ones with no resolvable geography.</li>
 *   <li>{@link Scoped} — a Nirdeshak / Sah-Nirdeshak (their Kshetra[s]), a
 *       Sanyojak (their Zone[s]), or the Regional Team (their City[ies]). Sees
 *       only entries whose resolved geography falls in one of those sets; the
 *       audit/oversight read is deliberately broader than the caller's write
 *       authority (it is not demographic-filtered).</li>
 *   <li>{@link Denied} — every tier below Nirdeshak (Sanchalak, Sah-Sanchalak,
 *       Nirikshak) and any caller who resolves to no scope at all. The BFF turns
 *       this into a 403; the feed is never queried.</li>
 * </ul>
 */
public sealed interface AuditScope {

    record Unrestricted() implements AuditScope {
    }

    record Scoped(Set<UUID> kshetraIds, Set<UUID> zoneIds, Set<UUID> cityIds) implements AuditScope {

        public boolean isEmpty() {
            return kshetraIds.isEmpty() && zoneIds.isEmpty() && cityIds.isEmpty();
        }
    }

    record Denied() implements AuditScope {
    }
}
