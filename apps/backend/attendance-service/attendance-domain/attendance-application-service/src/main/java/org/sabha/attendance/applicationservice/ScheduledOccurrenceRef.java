package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A Scheduled (or Rescheduled) Occurrence the auto-open scanner may act on.
 *
 * <p>{@code date} is the <em>effective</em> date (the rescheduled date when the
 * Occurrence was rescheduled, otherwise the standing occurrence date).
 * {@code rescheduledStartTime} is the per-Occurrence start-time override, or
 * {@code null} to fall back to the Sabha's standing schedule.</p>
 */
public record ScheduledOccurrenceRef(UUID occurrenceId, UUID sabhaId, LocalDate date,
                                     LocalTime rescheduledStartTime) {

    public ScheduledOccurrenceRef(UUID occurrenceId, UUID sabhaId, LocalDate date) {
        this(occurrenceId, sabhaId, date, null);
    }
}
