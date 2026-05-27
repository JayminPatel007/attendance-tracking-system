package org.sabha.sabha.dataaccess;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import org.sabha.common.SabhaSchedule;
import org.sabha.common.SabhaScheduleLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of the cross-context {@link SabhaScheduleLookup} port
 * (ADR-0019). Reads the standing weekly schedule from the {@code sabhas}
 * table. Returns {@link Optional#empty()} for sabhas with
 * {@code schedule_shape = 'MONTHLY_AD_HOC'} (no standing schedule).
 */
@Repository
public class JdbcSabhaScheduleLookup implements SabhaScheduleLookup {

    private final JdbcClient jdbc;

    public JdbcSabhaScheduleLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SabhaSchedule> findSchedule(UUID sabhaId) {
        return jdbc.sql("""
                SELECT day_of_week, start_time, end_time
                FROM sabhas
                WHERE id = ? AND schedule_shape = 'WEEKLY_RECURRING'
                """)
                .param(sabhaId)
                .query((rs, n) -> new SabhaSchedule(
                        toDayOfWeek(rs.getShort("day_of_week")),
                        rs.getObject("start_time", LocalTime.class),
                        rs.getObject("end_time", LocalTime.class)))
                .optional();
    }

    private static DayOfWeek toDayOfWeek(short stored) {
        // Database stores 0–6 (Sunday=0). DayOfWeek values are MONDAY=1..SUNDAY=7.
        return stored == 0 ? DayOfWeek.SUNDAY : DayOfWeek.of(stored);
    }
}
