package org.sabha.common;

import java.util.List;
import java.util.UUID;

/**
 * The one load of a caller's {@code role_assignments} rows. Lives in
 * common-domain beside {@link CallerResolver}, which it follows hop for hop: the
 * {@code role_assignments} table is identity's (ADR-0029), so the implementation
 * lives in identity-data-access, and the port lives here so the argument
 * resolver in {@code common-application} can reach it (ADR-0019).
 *
 * <p>Like {@link CallerResolver}, it has exactly one caller: the {@code
 * @CurrentUser} argument resolver, once per request. The two run <b>sequentially
 * and separately</b> — {@code users} by Keycloak subject, then this by {@code
 * users.id}. Collapsing them into one {@code LEFT JOIN} is available and
 * deliberately not taken: it would fuse the two questions ADR-0030 separated on
 * purpose, <em>who is this?</em> and <em>what may they do?</em>. The first is a
 * 403 when it fails; the second simply comes back empty.</p>
 *
 * <p>This port is the reason six identity adapters could be deleted. If it ever
 * grows a second method, or its return type stops being rows of one relation,
 * re-read {@link CallerAuthority}'s stopping rule before adding it.</p>
 */
public interface CallerAuthorityLookup {

    /** The caller's live role assignments — empty for a user holding no role. */
    List<RoleAssignment> assignmentsOf(UUID userId);
}
