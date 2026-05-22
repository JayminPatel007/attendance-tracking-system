package org.sabha.attendance.infrastructure;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceRepository;
import org.sabha.attendance.domain.OccurrenceState;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOccurrenceRepository implements OccurrenceRepository {

    private final JdbcClient jdbc;

    public JdbcOccurrenceRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Occurrence> findById(UUID occurrenceId) {
        return jdbc.sql("""
                SELECT id, sabha_id, occurrence_date, state
                FROM occurrences
                WHERE id = ?
                """)
                .param(occurrenceId)
                .query((rs, n) -> new Occurrence(
                        rs.getObject("id", UUID.class),
                        rs.getObject("sabha_id", UUID.class),
                        rs.getObject("occurrence_date", LocalDate.class),
                        OccurrenceState.valueOf(rs.getString("state"))))
                .optional();
    }
}
