package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.util.List;

/**
 * Driven port: read-side queries on Occurrences that the cron scanners need.
 * The JDBC implementation lives in attendance-data-access.
 */
public interface OccurrenceQueries {

    /** Scheduled (or Rescheduled) Occurrences — auto-open candidates. */
    List<OccurrenceSlotRef> findScheduledOnOrBefore(LocalDate date);

    /** Open-for-Marking Occurrences — auto-finalize candidates. */
    List<OccurrenceSlotRef> findOpenOnOrBefore(LocalDate date);
}
