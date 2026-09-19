package org.sabha.identity.dataaccess;

import java.util.Optional;
import java.util.UUID;

import org.sabha.common.SanchalakLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Resolves the Sanchalak currently running a Sabha from the {@code
 * role_assignments} table (owned by identity). The port lives in common-domain so
 * the attendance context's Authorization Engine can consult it across the
 * bounded-context seam (ADR-0019).
 *
 * <p>Was {@code JdbcRoleAssignmentLookup} and carried two further statements,
 * both keyed on {@code user_id}; ADR-0032 folded those into the single
 * caller-authority read at the request edge. What is left is keyed on {@code
 * sabha_id} and answers about a target, so it stays a query.</p>
 */
@Repository
public class JdbcSanchalakLookup implements SanchalakLookup {

    private final JdbcClient jdbc;

    public JdbcSanchalakLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
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
}
