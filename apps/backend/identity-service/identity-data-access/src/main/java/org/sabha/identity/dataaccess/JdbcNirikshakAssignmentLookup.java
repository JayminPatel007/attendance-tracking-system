package org.sabha.identity.dataaccess;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.sabha.common.NirikshakAssignmentLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Resolves a Nirikshak's explicit Sabha assignments from the {@code
 * nirikshak_sabha_assignments} table (owned by identity, Slice 14). The port lives
 * in common-domain so the attendance context's Authorization Engine can check the
 * Sanchalak-proxy scope across the bounded-context seam (ADR-0019).
 */
@Repository
public class JdbcNirikshakAssignmentLookup implements NirikshakAssignmentLookup {

    private final JdbcClient jdbc;

    public JdbcNirikshakAssignmentLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isAssignedTo(UUID userId, UUID sabhaId) {
        return jdbc.sql("""
                SELECT 1 FROM nirikshak_sabha_assignments
                WHERE nirikshak_user_id = ? AND sabha_id = ?
                """)
                .param(userId)
                .param(sabhaId)
                .query(Integer.class)
                .optional()
                .isPresent();
    }

    @Override
    public Set<UUID> sabhasAssignedTo(UUID userId) {
        return new LinkedHashSet<>(jdbc.sql("""
                SELECT sabha_id FROM nirikshak_sabha_assignments
                WHERE nirikshak_user_id = ?
                ORDER BY assigned_at
                """)
                .param(userId)
                .query((rs, n) -> rs.getObject("sabha_id", UUID.class))
                .list());
    }
}
