package org.sabha.attendance.dataaccess;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sabha.attendance.applicationservice.OccurrenceRepository;
import org.sabha.attendance.domain.AttendanceMarking;
import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.common.OptimisticLockException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcOccurrenceRepository implements OccurrenceRepository {

    private final JdbcClient jdbc;

    public JdbcOccurrenceRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Occurrence> findById(UUID occurrenceId) {
        Optional<OccurrenceRow> row = jdbc.sql("""
                SELECT id, sabha_id, occurrence_date, state, version
                FROM occurrences
                WHERE id = ?
                """)
                .param(occurrenceId)
                .query((rs, n) -> new OccurrenceRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("sabha_id", UUID.class),
                        rs.getObject("occurrence_date", LocalDate.class),
                        OccurrenceState.valueOf(rs.getString("state")),
                        rs.getLong("version")))
                .optional();
        if (row.isEmpty()) {
            return Optional.empty();
        }
        OccurrenceRow r = row.get();

        List<AttendanceMarking> markings = jdbc.sql("""
                SELECT id, person_id, present, marked_by_user_id
                FROM attendance_markings
                WHERE occurrence_id = ?
                """)
                .param(occurrenceId)
                .query((rs, n) -> new AttendanceMarking(
                        rs.getObject("id", UUID.class),
                        occurrenceId,
                        rs.getObject("person_id", UUID.class),
                        rs.getBoolean("present"),
                        rs.getObject("marked_by_user_id", UUID.class)))
                .list();

        return Optional.of(new Occurrence(r.id, r.sabhaId, r.date, r.state, r.version, markings));
    }

    @Override
    @Transactional
    public void save(Occurrence occurrence) {
        int rows = jdbc.sql("""
                UPDATE occurrences
                SET state = ?, version = version + 1
                WHERE id = ? AND version = ?
                """)
                .param(occurrence.state().name())
                .param(occurrence.id())
                .param(occurrence.version())
                .update();
        if (rows == 0) {
            throw new OptimisticLockException(occurrence.id());
        }

        for (AttendanceMarking m : occurrence.markings()) {
            jdbc.sql("""
                    INSERT INTO attendance_markings
                        (id, occurrence_id, person_id, present, marked_by_user_id)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (occurrence_id, person_id)
                    DO UPDATE SET present = EXCLUDED.present,
                                  marked_at = now(),
                                  marked_by_user_id = EXCLUDED.marked_by_user_id
                    """)
                    .param(m.id())
                    .param(occurrence.id())
                    .param(m.personId())
                    .param(m.present())
                    .param(m.markedByUserId())
                    .update();
        }
    }

    private record OccurrenceRow(UUID id, UUID sabhaId, LocalDate date, OccurrenceState state, Long version) {
    }
}
