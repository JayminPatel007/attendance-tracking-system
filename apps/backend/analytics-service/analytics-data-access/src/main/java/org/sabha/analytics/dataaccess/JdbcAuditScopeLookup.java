package org.sabha.analytics.dataaccess;

import java.util.Set;
import java.util.UUID;

import org.sabha.analytics.applicationservice.AuditScope;
import org.sabha.analytics.applicationservice.AuditScopeLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Resolves a caller's geographic audit scope from {@code role_assignments}
 * (ADR-0023). A Nirdeshak / Sah-Nirdeshak row carries the Kshetra it governs, a
 * Sanyojak row its Zone, a Regional Team row its City — one small query per tier,
 * collected into the {@link AuditScope.Scoped} sets. State-level membership (Sant,
 * MK) is decided above this in {@link org.sabha.analytics.applicationservice.AuditLogAccess};
 * this lookup only gathers the bounded geographic rows.
 */
@Repository
public class JdbcAuditScopeLookup implements AuditScopeLookup {

    private static final String KSHETRAS_SQL = """
            SELECT DISTINCT kshetra_id FROM role_assignments
            WHERE user_id = :userId AND role IN ('NIRDESHAK', 'SAH_NIRDESHAK') AND kshetra_id IS NOT NULL
            """;

    private static final String ZONES_SQL = """
            SELECT DISTINCT zone_id FROM role_assignments
            WHERE user_id = :userId AND role = 'SANYOJAK' AND zone_id IS NOT NULL
            """;

    private static final String CITIES_SQL = """
            SELECT DISTINCT city_id FROM role_assignments
            WHERE user_id = :userId AND role = 'REGIONAL_TEAM' AND city_id IS NOT NULL
            """;

    private final JdbcClient jdbc;

    public JdbcAuditScopeLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public AuditScope.Scoped scopeOf(UUID userId) {
        return new AuditScope.Scoped(ids(KSHETRAS_SQL, userId), ids(ZONES_SQL, userId), ids(CITIES_SQL, userId));
    }

    private Set<UUID> ids(String sql, UUID userId) {
        return Set.copyOf(jdbc.sql(sql).param("userId", userId).query(UUID.class).list());
    }
}
