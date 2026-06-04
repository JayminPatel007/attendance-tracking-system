package org.sabha.identity.dataaccess;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Resolves the operational roles a user holds on a specific Sabha from the
 * {@code role_assignments} table (owned by identity). The port lives in
 * common-domain so the attendance context's Authorization Engine can consult it
 * across the bounded-context seam (ADR-0019).
 *
 * <p>Role strings that do not map to a {@link Role} (e.g. Kshetra-level tiers
 * stored with a different shape) are skipped rather than failing the lookup.</p>
 */
@Repository
public class JdbcRoleAssignmentLookup implements RoleAssignmentLookup {

    private final JdbcClient jdbc;

    public JdbcRoleAssignmentLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Set<Role> rolesForUserOnSabha(UUID userId, UUID sabhaId) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        jdbc.sql("SELECT role FROM role_assignments WHERE user_id = ? AND sabha_id = ?")
                .param(userId)
                .param(sabhaId)
                .query((rs, n) -> rs.getString("role"))
                .list()
                .forEach(name -> toRole(name).ifPresent(roles::add));
        return roles;
    }

    @Override
    public Set<Role> rolesForUserOnKshetra(UUID userId, UUID kshetraId, String demographic) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        jdbc.sql("""
                SELECT role FROM role_assignments
                WHERE user_id = ? AND kshetra_id = ? AND demographic = ?
                """)
                .param(userId)
                .param(kshetraId)
                .param(demographic)
                .query((rs, n) -> rs.getString("role"))
                .list()
                .forEach(name -> toRole(name).ifPresent(roles::add));
        return roles;
    }

    private static Optional<Role> toRole(String name) {
        try {
            return Optional.of(Role.valueOf(name));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }
}
