package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.sabha.attendance.domain.Occurrence;
import org.sabha.common.SabhaSchedule;
import org.sabha.common.SabhaScheduleLookup;
import org.springframework.stereotype.Component;

/**
 * Auto-Open scanner (Slice 3 / PRD-0001). Driven by Spring scheduling on a
 * minutely cadence: finds Scheduled Occurrences whose computed scheduled start
 * Instant has passed and opens them through the single Occurrence write path.
 *
 * <p>Cross-context schedule resolution goes through {@link SabhaScheduleLookup}
 * (ADR-0019). Time-of-day on the Sabha is combined with the Occurrence date
 * and the Clock's zone to compute the start Instant.</p>
 */
@Component
public class AutoOpenScanner {

    private final OccurrenceQueries occurrenceQueries;
    private final SabhaScheduleLookup scheduleLookup;
    private final OccurrenceWriter writer;
    private final Clock clock;

    public AutoOpenScanner(
            OccurrenceQueries occurrenceQueries,
            SabhaScheduleLookup scheduleLookup,
            OccurrenceWriter writer,
            Clock clock) {
        this.occurrenceQueries = occurrenceQueries;
        this.scheduleLookup = scheduleLookup;
        this.writer = writer;
        this.clock = clock;
    }

    public void scan() {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, clock.getZone());
        for (ScheduledOccurrenceRef ref : occurrenceQueries.findScheduledOnOrBefore(today)) {
            // A per-Occurrence start time (a reschedule, or a monthly-ad-hoc
            // Occurrence's own slot) wins; otherwise fall back to the Sabha's
            // standing weekly schedule. Monthly Sabhas have no standing schedule,
            // so an Occurrence with neither is skipped.
            LocalTime startTime = ref.rescheduledStartTime();
            if (startTime == null) {
                Optional<SabhaSchedule> schedule = scheduleLookup.findSchedule(ref.sabhaId());
                if (schedule.isEmpty()) {
                    continue;
                }
                startTime = schedule.get().startTime();
            }
            Instant scheduledStartAt = ZonedDateTime.of(
                    ref.date(), startTime, clock.getZone()).toInstant();
            if (!scheduledStartAt.isAfter(now)) {
                writer.transition(ref.occurrenceId(), TransitionActor.system(),
                        OccurrenceAction.OPEN, null, Occurrence::open);
            }
        }
    }
}
