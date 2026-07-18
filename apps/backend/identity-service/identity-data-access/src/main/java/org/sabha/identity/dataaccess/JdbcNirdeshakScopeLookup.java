package org.sabha.identity.dataaccess;

import java.util.List;
import java.util.UUID;

import org.sabha.common.NirdeshakScopeLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Resolves the {@code (Kshetra, demographic)} scopes a user is a Nirdeshak of,
 * from the {@code role_assignments} table (owned by identity). Drives the sabha
 * context's "my Sabhas" listing across the bounded-context seam (ADR-0019,
 * ADR-0026): a Nirdeshak appointment carries both a {@code kshetra_id} and a
 * {@code demographic} (Slice 11). A user with no Nirdeshak scope resolves to an
 * empty list.
 */
@Repository
public class JdbcNirdeshakScopeLookup implements NirdeshakScopeLookup {

    private final JdbcClient jdbc;

    public JdbcNirdeshakScopeLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<NirdeshakScope> scopesOf(UUID userId) {
        return jdbc.sql("""
                SELECT kshetra_id, demographic FROM role_assignments
                WHERE user_id = ? AND role = 'NIRDESHAK'
                  AND kshetra_id IS NOT NULL AND demographic IS NOT NULL
                  AND revoked_at IS NULL
                """)
                .param(userId)
                .query((rs, n) -> new NirdeshakScope(
                        rs.getObject("kshetra_id", UUID.class), rs.getString("demographic")))
                .list();
    }
}
