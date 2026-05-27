package org.sabha.attendance.dataaccess;

import java.time.LocalDate;
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
        return jdbc.sql("""
                SELECT id, sabha_id, occurrence_date
                FROM occurrences
                WHERE state = 'SCHEDULED' AND occurrence_date <= ?
                """)
                .param(date)
                .query((rs, n) -> new ScheduledOccurrenceRef(
                        rs.getObject("id", UUID.class),
                        rs.getObject("sabha_id", UUID.class),
                        rs.getObject("occurrence_date", LocalDate.class)))
                .list();
    }

    @Override
    public List<OpenOccurrenceRef> findOpenOnOrBefore(LocalDate date) {
        return jdbc.sql("""
                SELECT id, sabha_id, occurrence_date
                FROM occurrences
                WHERE state = 'OPEN_FOR_MARKING' AND occurrence_date <= ?
                """)
                .param(date)
                .query((rs, n) -> new OpenOccurrenceRef(
                        rs.getObject("id", UUID.class),
                        rs.getObject("sabha_id", UUID.class),
                        rs.getObject("occurrence_date", LocalDate.class)))
                .list();
    }
}
