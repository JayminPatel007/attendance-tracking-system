package org.sabha.attendance.dataaccess;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import org.sabha.attendance.applicationservice.CurrentOccurrence;
import org.sabha.attendance.applicationservice.CurrentOccurrenceQuery;
import org.sabha.attendance.domain.OccurrenceState;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Read-side adapter for the occurrence-control screen: finds the Occurrence the
 * Sanchalak can currently shape on the Sabha they preside over. Cross-context
 * reads (identity ↔ attendance) are allowed via this port abstraction
 * (ADR-0019); cross-context writes still go through domain events.
 */
@Repository
public class JdbcCurrentOccurrenceQuery implements CurrentOccurrenceQuery {

    private final JdbcClient jdbc;

    public JdbcCurrentOccurrenceQuery(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<CurrentOccurrence> findShapeableForSanchalak(UUID sanchalakUserId) {
        return jdbc.sql("""
                SELECT o.id, o.sabha_id, o.occurrence_date, o.state,
                       o.venue_override, o.rescheduled_date,
                       o.rescheduled_start_time, o.rescheduled_end_time
                FROM role_assignments ra
                JOIN occurrences o
                       ON o.sabha_id = ra.sabha_id
                      AND o.state IN ('SCHEDULED', 'RESCHEDULED', 'CANCELLED')
                WHERE ra.user_id = ?
                  AND ra.role = 'SANCHALAK'
                ORDER BY o.occurrence_date DESC
                LIMIT 1
                """)
                .param(sanchalakUserId)
                .query((rs, n) -> new CurrentOccurrence(
                        rs.getObject("id", UUID.class),
                        rs.getObject("sabha_id", UUID.class),
                        rs.getObject("occurrence_date", LocalDate.class),
                        OccurrenceState.valueOf(rs.getString("state")),
                        rs.getString("venue_override"),
                        rs.getObject("rescheduled_date", LocalDate.class),
                        rs.getObject("rescheduled_start_time", LocalTime.class),
                        rs.getObject("rescheduled_end_time", LocalTime.class)))
                .optional();
    }
}
