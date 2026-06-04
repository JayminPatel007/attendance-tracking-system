package org.sabha.attendance.dataaccess;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.sabha.attendance.applicationservice.ProxyOccurrenceItem;
import org.sabha.attendance.applicationservice.ProxySabhaListItem;
import org.sabha.attendance.applicationservice.ProxySabhaQueries;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Read-side projection for the proxy Sabha picker (Slice 14). Lists the Sabhas
 * currently assigned to a Nirikshak ({@code nirikshak_sabha_assignments}) joined to
 * each Sabha's Sanchalak, with the informational "last seen" hint computed as the
 * {@code GREATEST} of the Sanchalak's login + sync ({@code user_activity}) and most
 * recent attendance marking — {@code GREATEST} ignores NULLs, so an absent signal
 * simply drops out and an all-absent Sanchalak yields {@code NULL}.
 *
 * <p>Like the reopen projection, this is a CQRS read over the single physical
 * schema (ADR-0008): it reads identity- and sabha-owned tables directly rather than
 * fanning out through cross-context ports, which the write paths use to keep the
 * seams compile-clean.</p>
 */
@Repository
public class JdbcProxySabhaQueries implements ProxySabhaQueries {

    private final JdbcClient jdbc;

    public JdbcProxySabhaQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ProxySabhaListItem> assignedSabhas(UUID nirikshakUserId) {
        return jdbc.sql("""
                SELECT s.id AS sabha_id,
                       s.sabha_kind || ' · ' || k.name AS sabha_label,
                       u.id AS sanchalak_user_id,
                       p.full_name AS sanchalak_name,
                       GREATEST(
                           ua.last_login_at,
                           ua.last_synced_at,
                           (SELECT MAX(am.marked_at) FROM attendance_markings am
                            WHERE am.marked_by_user_id = u.id)
                       ) AS last_seen_at
                FROM nirikshak_sabha_assignments na
                JOIN sabhas s ON s.id = na.sabha_id
                JOIN kshetras k ON k.id = s.kshetra_id
                LEFT JOIN role_assignments ra ON ra.sabha_id = s.id AND ra.role = 'SANCHALAK'
                LEFT JOIN users u ON u.id = ra.user_id
                LEFT JOIN persons p ON p.id = u.person_id
                LEFT JOIN user_activity ua ON ua.user_id = u.id
                WHERE na.nirikshak_user_id = ?
                ORDER BY na.assigned_at, s.id
                """)
                .param(nirikshakUserId)
                .query((rs, n) -> new ProxySabhaListItem(
                        rs.getObject("sabha_id", UUID.class),
                        rs.getString("sabha_label"),
                        rs.getObject("sanchalak_user_id", UUID.class),
                        rs.getString("sanchalak_name"),
                        instant(rs.getTimestamp("last_seen_at"))))
                .list();
    }

    @Override
    public List<ProxyOccurrenceItem> proxyOccurrences(UUID nirikshakUserId, UUID sabhaId) {
        return jdbc.sql("""
                SELECT o.id,
                       COALESCE(o.rescheduled_date, o.occurrence_date) AS effective_date,
                       o.state,
                       COALESCE(o.venue_override, s.standing_venue) AS venue
                FROM occurrences o
                JOIN sabhas s ON s.id = o.sabha_id
                WHERE o.sabha_id = ?
                  AND EXISTS (SELECT 1 FROM nirikshak_sabha_assignments na
                              WHERE na.nirikshak_user_id = ? AND na.sabha_id = o.sabha_id)
                ORDER BY effective_date DESC, o.id
                LIMIT 200
                """)
                .param(sabhaId)
                .param(nirikshakUserId)
                .query((rs, n) -> new ProxyOccurrenceItem(
                        rs.getObject("id", UUID.class),
                        rs.getObject("effective_date", LocalDate.class),
                        rs.getString("state"),
                        rs.getString("venue")))
                .list();
    }

    private static Instant instant(java.sql.Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
