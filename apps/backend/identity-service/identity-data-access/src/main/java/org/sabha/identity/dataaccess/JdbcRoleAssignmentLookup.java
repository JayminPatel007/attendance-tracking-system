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
        jdbc.sql("SELECT role FROM role_assignments WHERE user_id = ? AND sabha_id = ? AND revoked_at IS NULL")
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
                  AND revoked_at IS NULL
                """)
                .param(userId)
                .param(kshetraId)
                .param(demographic)
                .query((rs, n) -> rs.getString("role"))
                .list()
                .forEach(name -> toRole(name).ifPresent(roles::add));
        return roles;
    }

    @Override
    public Optional<UUID> sanchalakOf(UUID sabhaId) {
        return jdbc.sql("""
                SELECT user_id FROM role_assignments
                WHERE sabha_id = ? AND role = 'SANCHALAK'
                  AND revoked_at IS NULL
                LIMIT 1
                """)
                .param(sabhaId)
                .query((rs, n) -> rs.getObject("user_id", UUID.class))
                .optional();
    }

    private static Optional<Role> toRole(String name) {
        try {
            return Optional.of(Role.valueOf(name));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }
}
