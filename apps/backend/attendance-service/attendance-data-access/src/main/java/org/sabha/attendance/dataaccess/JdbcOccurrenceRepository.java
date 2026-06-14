package org.sabha.attendance.dataaccess;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sabha.attendance.applicationservice.OccurrenceInsert;
import org.sabha.attendance.applicationservice.OccurrenceRepository;
import org.sabha.attendance.domain.AttendanceMarking;
import org.sabha.attendance.domain.MarkingType;
import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.common.OptimisticLockException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOccurrenceRepository implements OccurrenceRepository, OccurrenceInsert {

    private final JdbcClient jdbc;

    public JdbcOccurrenceRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Occurrence> findById(UUID occurrenceId) {
        Optional<OccurrenceRow> row = jdbc.sql("""
                SELECT id, sabha_id, occurrence_date, state, version,
                       venue_override, rescheduled_date, rescheduled_start_time, rescheduled_end_time
                FROM occurrences
                WHERE id = ?
                """)
                .param(occurrenceId)
                .query((rs, n) -> new OccurrenceRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("sabha_id", UUID.class),
                        rs.getObject("occurrence_date", LocalDate.class),
                        OccurrenceState.valueOf(rs.getString("state")),
                        rs.getLong("version"),
                        rs.getString("venue_override"),
                        rs.getObject("rescheduled_date", LocalDate.class),
                        rs.getObject("rescheduled_start_time", LocalTime.class),
                        rs.getObject("rescheduled_end_time", LocalTime.class)))
                .optional();
        if (row.isEmpty()) {
            return Optional.empty();
        }
        OccurrenceRow r = row.get();

        List<AttendanceMarking> markings = jdbc.sql("""
                SELECT id, person_id, present, marked_by_user_id, client_marked_at, marking_type
                FROM attendance_markings
                WHERE occurrence_id = ?
                """)
                .param(occurrenceId)
                .query((rs, n) -> new AttendanceMarking(
                        rs.getObject("id", UUID.class),
                        occurrenceId,
                        rs.getObject("person_id", UUID.class),
                        rs.getBoolean("present"),
                        rs.getObject("marked_by_user_id", UUID.class),
                        rs.getTimestamp("client_marked_at").toInstant(),
                        MarkingType.valueOf(rs.getString("marking_type"))))
                .list();

        Occurrence occurrence = new Occurrence(r.id, r.sabhaId, r.date, r.state, r.version, markings);
        occurrence.restoreShaping(r.venueOverride, r.rescheduledDate,
                r.rescheduledStartTime, r.rescheduledEndTime);
        return Optional.of(occurrence);
    }

    @Override
    public void add(Occurrence occurrence) {
        jdbc.sql("""
                INSERT INTO occurrences
                    (id, sabha_id, occurrence_date, state,
                     venue_override, rescheduled_date, rescheduled_start_time, rescheduled_end_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)
                .param(occurrence.id())
                .param(occurrence.sabhaId())
                .param(occurrence.date())
                .param(occurrence.state().name())
                .param(occurrence.venueOverride())
                .param(occurrence.rescheduledDate())
                .param(occurrence.rescheduledStartTime())
                .param(occurrence.rescheduledEndTime())
                .update();
    }

    @Override
    public void save(Occurrence occurrence) {
        int rows = jdbc.sql("""
                UPDATE occurrences
                SET state = ?,
                    venue_override = ?,
                    rescheduled_date = ?,
                    rescheduled_start_time = ?,
                    rescheduled_end_time = ?,
                    version = version + 1
                WHERE id = ? AND version = ?
                """)
                .param(occurrence.state().name())
                .param(occurrence.venueOverride())
                .param(occurrence.rescheduledDate())
                .param(occurrence.rescheduledStartTime())
                .param(occurrence.rescheduledEndTime())
                .param(occurrence.id())
                .param(occurrence.version())
                .update();
        if (rows == 0) {
            throw new OptimisticLockException(occurrence.id());
        }

        for (AttendanceMarking m : occurrence.pullPendingMarkings()) {
            jdbc.sql("""
                    INSERT INTO attendance_markings
                        (id, occurrence_id, person_id, present, marked_by_user_id, client_marked_at, marking_type)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (occurrence_id, person_id)
                    DO UPDATE SET present = EXCLUDED.present,
                                  marked_at = now(),
                                  marked_by_user_id = EXCLUDED.marked_by_user_id,
                                  client_marked_at = EXCLUDED.client_marked_at,
                                  marking_type = EXCLUDED.marking_type
                    """)
                    .param(m.id())
                    .param(occurrence.id())
                    .param(m.personId())
                    .param(m.present())
                    .param(m.markedByUserId())
                    .param(Timestamp.from(m.clientMarkedAt()))
                    .param(m.markingType().name())
                    .update();
        }
    }

    private record OccurrenceRow(UUID id, UUID sabhaId, LocalDate date, OccurrenceState state, Long version,
                                 String venueOverride, LocalDate rescheduledDate,
                                 LocalTime rescheduledStartTime, LocalTime rescheduledEndTime) {
    }
}
