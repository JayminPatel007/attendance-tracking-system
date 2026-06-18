package org.sabha.identity.domain;

import java.util.Set;

import org.sabha.common.Role;

/**
 * The authority facts {@link VisibleSections} maps to a web shell sidebar: the
 * two oversight memberships that sit outside the operational {@link Role} enum
 * (Madhyastha Karyalaya and Regional Team), whether the BFF's audit engine
 * admits the caller to any audit scope, and the operational roles they hold.
 *
 * <p>Bundling these as one value keeps {@code forMember} call sites
 * self-documenting and immune to silently transposing two same-typed booleans.
 * The application service ({@code WebSessionService}) is the single producer —
 * it resolves each fact through its own port and snapshots them here.</p>
 */
public record MemberAuthority(
        boolean madhyasthaKaryalaya,
        boolean regionalTeam,
        boolean canReadAudit,
        Set<Role> operationalRoles) {
}
