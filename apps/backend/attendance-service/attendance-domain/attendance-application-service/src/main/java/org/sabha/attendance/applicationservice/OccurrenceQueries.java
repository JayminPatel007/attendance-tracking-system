package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.util.List;

/**
 * Driven port: read-side queries on Occurrences that the cron scanners need.
 * The JDBC implementation lives in attendance-data-access.
 */
public interface OccurrenceQueries {

    List<ScheduledOccurrenceRef> findScheduledOnOrBefore(LocalDate date);

    List<OpenOccurrenceRef> findOpenOnOrBefore(LocalDate date);
}
