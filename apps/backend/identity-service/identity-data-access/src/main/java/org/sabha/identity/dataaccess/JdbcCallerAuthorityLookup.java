package org.sabha.identity.dataaccess;

import java.util.List;
import java.util.UUID;

import org.sabha.common.CallerAuthorityLookup;
import org.sabha.common.RoleAssignment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The one authority read of {@code role_assignments} (ADR-0029, ADR-0032). Six
 * adapters collapsed into this statement — {@code JdbcNirdeshakScopeLookup},
 * {@code JdbcSanyojakZoneLookup}, {@code JdbcRegionalTeamCityLookup}, {@code
 * JdbcUserRolesLookup}, {@code JdbcAppointerAuthorityLookup} and {@code
 * JdbcAuditScopeLookup} (itself three {@code SELECT DISTINCT}s) — because all
 * nine questions they answered were the same statement differing only in which
 * scope column they projected and whether they returned a list or an {@code
 * EXISTS}. Those differences are filters, and filters belong in the domain.
 *
 * <p>No {@code role} predicate and no scope predicate: the rows are selected by
 * caller alone and every distinction is drawn in {@link
 * org.sabha.common.CallerAuthority}. {@code revoked_at IS NULL} is the one filter
 * that stays here, because a revoked row is not a fact the domain should have to
 * remember to exclude.</p>
 *
 * <p>Runs once per request at the edge. It is unindexed-scan-shaped only in the
 * sense that it reads every row for one user; {@code role_assignments} is keyed
 * by {@code user_id} and a caller holds single-digit rows.</p>
 */
@Repository
public class JdbcCallerAuthorityLookup implements CallerAuthorityLookup {

    private static final String ASSIGNMENTS_SQL = """
            SELECT role, sabha_id, kshetra_id, zone_id, city_id, demographic
            FROM role_assignments
            WHERE user_id = ? AND revoked_at IS NULL
            """;

    private final JdbcClient jdbc;

    public JdbcCallerAuthorityLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<RoleAssignment> assignmentsOf(UUID userId) {
        return jdbc.sql(ASSIGNMENTS_SQL)
                .param(userId)
                .query((rs, n) -> new RoleAssignment(
                        rs.getString("role"),
                        rs.getObject("sabha_id", UUID.class),
                        rs.getObject("kshetra_id", UUID.class),
                        rs.getObject("zone_id", UUID.class),
                        rs.getObject("city_id", UUID.class),
                        rs.getString("demographic")))
                .list();
    }
}
