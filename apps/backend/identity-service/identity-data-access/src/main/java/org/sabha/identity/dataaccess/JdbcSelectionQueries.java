package org.sabha.identity.dataaccess;

import java.util.List;
import java.util.UUID;

import org.sabha.identity.applicationservice.PendingNominationItem;
import org.sabha.identity.applicationservice.SelectedPersonItem;
import org.sabha.identity.applicationservice.SelectionQueries;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter for the Nirdeshak's pending-nomination queue (ADR-0006). Scopes
 * with an EXISTS over the caller's NIRDESHAK role rows matched on the nomination's
 * (kshetra_id, demographic) — the same scope-as-SQL-predicate pattern the reopen
 * and re-engagement read models use, so there is no Java grant model to keep in
 * sync. The track-shared higher tier means one NIRDESHAK row covers both Regular
 * and the selective track for that demographic.
 */
@Repository
public class JdbcSelectionQueries implements SelectionQueries {

    private final JdbcClient jdbc;

    public JdbcSelectionQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<PendingNominationItem> pendingQueueFor(UUID nirdeshakUserId) {
        return jdbc.sql("""
                SELECT n.id, n.person_id, p.full_name AS person_name,
                       n.regular_sabha_id, n.selective_sabha_id,
                       n.demographic, n.track,
                       n.nominated_by, nu_p.full_name AS nominated_by_name,
                       n.nominated_at
                FROM selection_nominations n
                JOIN persons p ON p.id = n.person_id
                JOIN users nu ON nu.id = n.nominated_by
                JOIN persons nu_p ON nu_p.id = nu.person_id
                WHERE n.status = 'PENDING'
                  AND EXISTS (
                      SELECT 1 FROM role_assignments ra
                      WHERE ra.user_id = ?
                        AND ra.role = 'NIRDESHAK'
                        AND ra.kshetra_id = n.kshetra_id
                        AND ra.demographic = n.demographic
                  )
                ORDER BY n.nominated_at
                """)
                .param(nirdeshakUserId)
                .query((rs, rn) -> new PendingNominationItem(
                        rs.getObject("id", UUID.class),
                        rs.getObject("person_id", UUID.class),
                        rs.getString("person_name"),
                        rs.getObject("regular_sabha_id", UUID.class),
                        rs.getObject("selective_sabha_id", UUID.class),
                        rs.getString("demographic"),
                        rs.getString("track"),
                        rs.getObject("nominated_by", UUID.class),
                        rs.getString("nominated_by_name"),
                        rs.getTimestamp("nominated_at").toInstant()))
                .list();
    }

    @Override
    public List<SelectedPersonItem> selectedFor(UUID nirdeshakUserId) {
        return jdbc.sql("""
                SELECT n.id, n.person_id, p.full_name AS person_name,
                       n.selective_sabha_id, n.demographic, n.track,
                       n.decided_by, du_p.full_name AS decided_by_name,
                       n.decided_at
                FROM selection_nominations n
                JOIN persons p ON p.id = n.person_id
                JOIN users du ON du.id = n.decided_by
                JOIN persons du_p ON du_p.id = du.person_id
                WHERE n.status = 'APPROVED'
                  AND EXISTS (
                      SELECT 1 FROM role_assignments ra
                      WHERE ra.user_id = ?
                        AND ra.role = 'NIRDESHAK'
                        AND ra.kshetra_id = n.kshetra_id
                        AND ra.demographic = n.demographic
                  )
                ORDER BY n.decided_at DESC
                """)
                .param(nirdeshakUserId)
                .query((rs, rn) -> new SelectedPersonItem(
                        rs.getObject("id", UUID.class),
                        rs.getObject("person_id", UUID.class),
                        rs.getString("person_name"),
                        rs.getObject("selective_sabha_id", UUID.class),
                        rs.getString("demographic"),
                        rs.getString("track"),
                        rs.getObject("decided_by", UUID.class),
                        rs.getString("decided_by_name"),
                        rs.getTimestamp("decided_at").toInstant()))
                .list();
    }
}
