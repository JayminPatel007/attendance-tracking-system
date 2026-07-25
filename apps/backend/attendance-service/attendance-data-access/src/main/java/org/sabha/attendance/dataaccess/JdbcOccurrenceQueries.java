package org.sabha.attendance.dataaccess;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.sabha.attendance.applicationservice.OccurrenceQueries;
import org.sabha.attendance.applicationservice.OccurrenceSlotRef;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOccurrenceQueries implements OccurrenceQueries {

    private final JdbcClient jdbc;

    public JdbcOccurrenceQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<OccurrenceSlotRef> findScheduledOnOrBefore(LocalDate date) {
        // Rescheduled Occurrences are open candidates too; their effective date/time
        // come from the rescheduled overrides (ADR-0001), falling back to the standing
        // occurrence date when not rescheduled.
        return jdbc.sql(slotRefQuery("state IN ('SCHEDULED', 'RESCHEDULED')"))
                .param(date)
                .query(JdbcOccurrenceQueries::toSlotRef)
                .list();
    }

    @Override
    public List<OccurrenceSlotRef> findOpenOnOrBefore(LocalDate date) {
        return jdbc.sql(slotRefQuery("state = 'OPEN_FOR_MARKING'"))
                .param(date)
                .query(JdbcOccurrenceQueries::toSlotRef)
                .list();
    }

    private static String slotRefQuery(String statePredicate) {
        return """
                SELECT id, sabha_id,
                       COALESCE(rescheduled_date, occurrence_date) AS effective_date,
                       rescheduled_start_time, rescheduled_end_time
                FROM occurrences
                WHERE %s
                  AND COALESCE(rescheduled_date, occurrence_date) <= ?
                """.formatted(statePredicate);
    }

    private static OccurrenceSlotRef toSlotRef(ResultSet rs, int rowNum) throws SQLException {
        return new OccurrenceSlotRef(
                rs.getObject("id", UUID.class),
                rs.getObject("sabha_id", UUID.class),
                rs.getObject("effective_date", LocalDate.class),
                rs.getObject("rescheduled_start_time", LocalTime.class),
                rs.getObject("rescheduled_end_time", LocalTime.class));
    }
}
