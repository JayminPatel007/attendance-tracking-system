package org.sabha.attendance.dataaccess;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.sabha.attendance.applicationservice.OccurrenceQueries;
import org.sabha.attendance.applicationservice.OpenOccurrenceRef;
import org.sabha.attendance.applicationservice.ScheduledOccurrenceRef;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOccurrenceQueries implements OccurrenceQueries {

    private final JdbcClient jdbc;

    public JdbcOccurrenceQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ScheduledOccurrenceRef> findScheduledOnOrBefore(LocalDate date) {
        // Rescheduled Occurrences are open candidates too; their effective date/time
        // come from the rescheduled overrides (ADR-0001), falling back to the standing
        // occurrence date when not rescheduled.
        return jdbc.sql("""
                SELECT id, sabha_id,
                       COALESCE(rescheduled_date, occurrence_date) AS effective_date,
                       rescheduled_start_time
                FROM occurrences
                WHERE state IN ('SCHEDULED', 'RESCHEDULED')
                  AND COALESCE(rescheduled_date, occurrence_date) <= ?
                """)
                .param(date)
                .query((rs, n) -> new ScheduledOccurrenceRef(
                        rs.getObject("id", UUID.class),
                        rs.getObject("sabha_id", UUID.class),
                        rs.getObject("effective_date", LocalDate.class),
                        rs.getObject("rescheduled_start_time", LocalTime.class)))
                .list();
    }

    @Override
    public List<OpenOccurrenceRef> findOpenOnOrBefore(LocalDate date) {
        return jdbc.sql("""
                SELECT id, sabha_id,
                       COALESCE(rescheduled_date, occurrence_date) AS effective_date,
                       rescheduled_end_time
                FROM occurrences
                WHERE state = 'OPEN_FOR_MARKING'
                  AND COALESCE(rescheduled_date, occurrence_date) <= ?
                """)
                .param(date)
                .query((rs, n) -> new OpenOccurrenceRef(
                        rs.getObject("id", UUID.class),
                        rs.getObject("sabha_id", UUID.class),
                        rs.getObject("effective_date", LocalDate.class),
                        rs.getObject("rescheduled_end_time", LocalTime.class)))
                .list();
    }
}
