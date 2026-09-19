package org.sabha.common;

import java.util.UUID;

/**
 * One row of the caller's {@code role_assignments} — the relation identity owns
 * (ADR-0029) and the only relation {@link CallerAuthority} is built from.
 *
 * <p>{@code role} stays the <b>wire string</b>, deliberately. Four overlapping
 * enums partition that column differently — {@link Role} (6), {@code
 * AppointableRole} (8), {@link OversightRole} (2) and {@link VisibilityTier} —
 * and none covers all nine values it holds. Parsing here would force a choice of
 * taxonomy on every reader; instead {@link CallerAuthority} keeps its rows
 * private and parses only where a named question's return type needs a {@code
 * Role}. The taxonomy problem stays sealed behind that boundary (issue #232).</p>
 *
 * <p>Scope columns are mutually exclusive in practice but not in the schema, so
 * all four are carried and each named question matches on the one it means. A
 * revoked row never reaches here: the loader filters {@code revoked_at IS NULL},
 * so every row in a {@code CallerAuthority} is live as of the request edge.</p>
 */
public record RoleAssignment(
        String role,
        UUID sabhaId,
        UUID kshetraId,
        UUID zoneId,
        UUID cityId,
        String demographic) {
}
