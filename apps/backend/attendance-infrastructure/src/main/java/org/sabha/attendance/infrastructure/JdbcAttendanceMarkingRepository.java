package org.sabha.attendance.infrastructure;

import org.sabha.attendance.domain.AttendanceMarking;
import org.sabha.attendance.domain.AttendanceMarkingRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAttendanceMarkingRepository implements AttendanceMarkingRepository {

    private final JdbcClient jdbc;

    public JdbcAttendanceMarkingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsert(AttendanceMarking marking) {
        jdbc.sql("""
                INSERT INTO attendance_markings (id, occurrence_id, person_id, present, marked_by_user_id)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (occurrence_id, person_id)
                DO UPDATE SET present = EXCLUDED.present,
                              marked_at = now(),
                              marked_by_user_id = EXCLUDED.marked_by_user_id
                """)
                .param(marking.id())
                .param(marking.occurrenceId())
                .param(marking.personId())
                .param(marking.present())
                .param(marking.markedByUserId())
                .update();
    }
}
