package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * An Open-for-Marking Occurrence the auto-finalize scanner may act on.
 *
 * <p>{@code date} is the <em>effective</em> date (rescheduled when applicable).
 * {@code rescheduledEndTime} is the per-Occurrence end-time override used to
 * compute the finalize cutoff, or {@code null} to fall back to the Sabha's
 * standing schedule.</p>
 */
public record OpenOccurrenceRef(UUID occurrenceId, UUID sabhaId, LocalDate date,
                                LocalTime rescheduledEndTime) {

    public OpenOccurrenceRef(UUID occurrenceId, UUID sabhaId, LocalDate date) {
        this(occurrenceId, sabhaId, date, null);
    }
}
