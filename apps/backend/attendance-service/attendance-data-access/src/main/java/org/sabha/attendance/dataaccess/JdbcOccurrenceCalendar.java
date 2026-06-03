package org.sabha.attendance.dataaccess;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import org.sabha.attendance.applicationservice.OccurrenceCalendar;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of {@link OccurrenceCalendar} (ADR-0012): existence of an
 * Occurrence for a Sabha on a date, keeping weekly materialization idempotent.
 * Matches on the standing {@code occurrence_date} — the date the cron writes.
 */
@Repository
public class JdbcOccurrenceCalendar implements OccurrenceCalendar {

    private final JdbcClient jdbc;

    public JdbcOccurrenceCalendar(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean exists(UUID sabhaId, LocalDate date) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM occurrences WHERE sabha_id = ? AND occurrence_date = ?
                )
                """)
                .param(sabhaId)
                .param(date)
                .query(Boolean.class)
                .single();
    }

    @Override
    public boolean existsInMonth(UUID sabhaId, YearMonth month) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM occurrences
                    WHERE sabha_id = ? AND occurrence_date >= ? AND occurrence_date <= ?
                )
                """)
                .param(sabhaId)
                .param(month.atDay(1))
                .param(month.atEndOfMonth())
                .query(Boolean.class)
                .single();
    }
}
