package org.sabha.attendance.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sabha.attendance.domain.CurrentRoster;
import org.sabha.attendance.domain.CurrentRoster.OccurrenceView;
import org.sabha.attendance.domain.CurrentRoster.RosterEntry;
import org.sabha.attendance.domain.CurrentRosterQuery;
import org.sabha.attendance.domain.OccurrenceState;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Read-side adapter: joins identity (role_assignments), attendance
 * (occurrences, attendance_markings), and sabha (home_sabhas, persons) to
 * answer "what is the Sanchalak looking at right now?". Cross-context reads
 * are explicitly allowed via this port abstraction (ADR-0017); cross-context
 * writes still go through domain events or common-domain.
 */
@Repository
public class JdbcCurrentRosterQuery implements CurrentRosterQuery {

    private final JdbcClient jdbc;

    public JdbcCurrentRosterQuery(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<CurrentRoster> findForSanchalak(UUID sanchalakUserId) {
        Optional<OccurrenceView> openOccurrence = jdbc.sql("""
                SELECT o.id AS occurrence_id, o.occurrence_date, o.state, o.sabha_id
                FROM role_assignments ra
                JOIN occurrences o
                       ON o.sabha_id = ra.sabha_id
                      AND o.state = 'OPEN_FOR_MARKING'
                WHERE ra.user_id = ?
                  AND ra.role = 'SANCHALAK'
                ORDER BY o.occurrence_date DESC
                LIMIT 1
                """)
                .param(sanchalakUserId)
                .query((rs, n) -> new OccurrenceView(
                        rs.getObject("occurrence_id", UUID.class),
                        rs.getObject("occurrence_date", LocalDate.class),
                        OccurrenceState.valueOf(rs.getString("state")),
                        rs.getObject("sabha_id", UUID.class)))
                .optional();

        if (openOccurrence.isEmpty()) {
            return Optional.empty();
        }
        OccurrenceView occ = openOccurrence.get();

        List<RosterEntry> roster = jdbc.sql("""
                SELECT p.id AS person_id, p.full_name, am.present
                FROM home_sabhas hs
                JOIN persons p ON p.id = hs.person_id
                LEFT JOIN attendance_markings am
                       ON am.occurrence_id = ? AND am.person_id = p.id
                WHERE hs.sabha_id = ?
                ORDER BY p.id
                """)
                .param(occ.id())
                .param(occ.sabhaId())
                .query((rs, n) -> {
                    boolean present = rs.getBoolean("present");
                    Boolean nullablePresent = rs.wasNull() ? null : present;
                    return new RosterEntry(
                            rs.getObject("person_id", UUID.class),
                            rs.getString("full_name"),
                            nullablePresent);
                })
                .list();

        return Optional.of(new CurrentRoster(occ, roster));
    }
}
