package org.sabha.sabha.dataaccess;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.sabha.common.SabhaSchedule;
import org.sabha.common.WeeklySabhaCatalog;
import org.sabha.common.WeeklySabhaRef;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of the cross-context {@link WeeklySabhaCatalog} port
 * (ADR-0012, ADR-0019). Returns every {@code WEEKLY_RECURRING} Sabha with its
 * standing slot; {@code MONTHLY_AD_HOC} Sabhas are excluded so the materialization
 * cron never rolls Occurrences forward for them.
 */
@Repository
public class JdbcWeeklySabhaCatalog implements WeeklySabhaCatalog {

    private final JdbcClient jdbc;

    public JdbcWeeklySabhaCatalog(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<WeeklySabhaRef> findAllWeekly() {
        return jdbc.sql("""
                SELECT id, day_of_week, start_time, end_time
                FROM sabhas
                WHERE schedule_shape = 'WEEKLY_RECURRING'
                """)
                .query((rs, n) -> new WeeklySabhaRef(
                        rs.getObject("id", java.util.UUID.class),
                        new SabhaSchedule(
                                toDayOfWeek(rs.getShort("day_of_week")),
                                rs.getObject("start_time", LocalTime.class),
                                rs.getObject("end_time", LocalTime.class))))
                .list();
    }

    private static DayOfWeek toDayOfWeek(short stored) {
        // Database stores 0–6 (Sunday=0). DayOfWeek values are MONDAY=1..SUNDAY=7.
        return stored == 0 ? DayOfWeek.SUNDAY : DayOfWeek.of(stored);
    }
}
