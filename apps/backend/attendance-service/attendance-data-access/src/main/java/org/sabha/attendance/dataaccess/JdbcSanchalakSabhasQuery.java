package org.sabha.attendance.dataaccess;

import java.util.List;
import java.util.UUID;

import org.sabha.attendance.applicationservice.SanchalakSabhasQuery;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Read-side adapter for the mobile's monthly Occurrence-create entry point:
 * lists the monthly-ad-hoc Sabhas a Sanchalak presides over (ADR-0012). Joins
 * {@code role_assignments} (identity) with {@code sabhas} — a cross-context
 * read allowed via this port abstraction (ADR-0019).
 */
@Repository
public class JdbcSanchalakSabhasQuery implements SanchalakSabhasQuery {

    private final JdbcClient jdbc;

    public JdbcSanchalakSabhasQuery(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<MonthlyAdHocSabha> monthlyAdHocFor(UUID sanchalakUserId) {
        return jdbc.sql("""
                SELECT s.id, s.sabha_kind, s.standing_venue
                FROM role_assignments ra
                JOIN sabhas s ON s.id = ra.sabha_id
                WHERE ra.user_id = ?
                  AND ra.role = 'SANCHALAK'
                  AND ra.revoked_at IS NULL
                  AND s.schedule_shape = 'MONTHLY_AD_HOC'
                ORDER BY s.standing_venue
                """)
                .param(sanchalakUserId)
                .query((rs, n) -> new MonthlyAdHocSabha(
                        rs.getObject("id", UUID.class),
                        rs.getString("sabha_kind"),
                        rs.getString("standing_venue")))
                .list();
    }
}
