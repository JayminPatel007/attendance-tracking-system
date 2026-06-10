package org.sabha.analytics.applicationservice;

import java.util.List;

/**
 * Read port over the audit feed (ADR-0023, Slice 19). The whole UNION across the
 * source tables, the geography resolution, the scope predicate, and the actor-name
 * join live behind this one method in {@code analytics-data-access}; callers see
 * only a scoped, filtered, newest-first list of {@link AuditEntry}.
 *
 * <p>The {@link AuditScope} is the engine's decision ({@link AuditLogAccess}); a
 * {@link AuditScope.Denied} scope must be rejected before calling this — the
 * adapter treats it as matching nothing rather than as an error.</p>
 */
public interface AuditLogQueries {

    List<AuditEntry> find(AuditScope scope, AuditFilter filter);
}
