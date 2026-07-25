package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * An Occurrence the cron scanners may act on, carrying exactly the inputs its
 * Effective Slot is resolved from (see {@link EffectiveSlotResolver}).
 *
 * <p>{@code date} is the <em>effective</em> date (the rescheduled date when the
 * Occurrence was rescheduled, otherwise the standing occurrence date). The
 * override times are the per-Occurrence start/end, or {@code null} to fall back
 * to the Sabha's standing schedule.</p>
 */
public record OccurrenceSlotRef(UUID occurrenceId, UUID sabhaId, LocalDate date,
                                LocalTime overrideStartTime, LocalTime overrideEndTime) {

    public OccurrenceSlotRef(UUID occurrenceId, UUID sabhaId, LocalDate date) {
        this(occurrenceId, sabhaId, date, null, null);
    }
}
