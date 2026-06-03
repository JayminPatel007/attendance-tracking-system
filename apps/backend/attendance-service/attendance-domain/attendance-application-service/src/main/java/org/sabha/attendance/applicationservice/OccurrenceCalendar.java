package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Driven port over the Occurrence calendar (ADR-0012). {@link #exists} keeps the
 * weekly materialization cron idempotent — it skips dates already present —
 * without depending on the table's unique constraint. {@link #existsInMonth}
 * drives the monthly compliance nudge. The JDBC implementation lives in
 * attendance-data-access.
 */
public interface OccurrenceCalendar {

    boolean exists(UUID sabhaId, LocalDate date);

    boolean existsInMonth(UUID sabhaId, YearMonth month);
}
