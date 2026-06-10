package org.sabha.analytics.applicationservice;

import java.util.UUID;

/**
 * Read port resolving a caller's <em>geographic</em> audit scope (ADR-0023): the
 * Kshetras they hold as Nirdeshak / Sah-Nirdeshak, the Zones they coordinate as
 * Sanyojak, and the Cities they oversee as Regional Team. Returns the raw
 * {@link AuditScope.Scoped} sets; {@link AuditLogAccess} layers the
 * State-wide ({@code Unrestricted}) and {@code Denied} decisions on top. Reads
 * {@code role_assignments} directly, the way the analytics read models already
 * couple to that table (ADR-0008); adapter in {@code analytics-data-access}.
 */
public interface AuditScopeLookup {

    AuditScope.Scoped scopeOf(UUID userId);
}
