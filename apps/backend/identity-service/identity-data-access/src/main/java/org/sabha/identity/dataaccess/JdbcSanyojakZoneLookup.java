package org.sabha.identity.dataaccess;

import java.util.List;
import java.util.UUID;

import org.sabha.common.SanyojakZoneLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Resolves the Zones a user is a Sanyojak of, from the {@code role_assignments}
 * table (owned by identity). Drives Kshetra-creation authorization in the sabha
 * context across the bounded-context seam (ADR-0019, ADR-0009). The
 * {@code zone_id} column is populated by role appointment (Slice 11); a user
 * with no Sanyojak Zone scope resolves to an empty list.
 */
@Repository
public class JdbcSanyojakZoneLookup implements SanyojakZoneLookup {

    private static final String SANYOJAK_ROLE = "SANYOJAK";

    private final JdbcClient jdbc;

    public JdbcSanyojakZoneLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<UUID> zonesOf(UUID userId) {
        return jdbc.sql("""
                SELECT zone_id FROM role_assignments
                WHERE user_id = ? AND role = ? AND zone_id IS NOT NULL
                """)
                .param(userId)
                .param(SANYOJAK_ROLE)
                .query((rs, n) -> rs.getObject("zone_id", UUID.class))
                .list();
    }
}
