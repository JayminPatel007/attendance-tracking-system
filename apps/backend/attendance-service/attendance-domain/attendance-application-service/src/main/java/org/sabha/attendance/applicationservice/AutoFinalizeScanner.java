package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.sabha.attendance.domain.Occurrence;
import org.sabha.common.SabhaSchedule;
import org.sabha.common.SabhaScheduleLookup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Auto-Finalize scanner (Slice 3 / PRD-0001). Driven by Spring scheduling on
 * an hourly cadence: finds Open-for-Marking Occurrences whose scheduled end
 * Instant plus a grace period (24h per ADR-0001) has passed, and finalizes them
 * through the single Occurrence write path.
 */
@Component
public class AutoFinalizeScanner {

    private final OccurrenceQueries occurrenceQueries;
    private final SabhaScheduleLookup scheduleLookup;
    private final OccurrenceWriter writer;
    private final Clock clock;
    private final Duration gracePeriod;

    public AutoFinalizeScanner(
            OccurrenceQueries occurrenceQueries,
            SabhaScheduleLookup scheduleLookup,
            OccurrenceWriter writer,
            Clock clock,
            @Value("${sabha.attendance.auto-finalize.grace:PT24H}") Duration gracePeriod) {
        this.occurrenceQueries = occurrenceQueries;
        this.scheduleLookup = scheduleLookup;
        this.writer = writer;
        this.clock = clock;
        this.gracePeriod = gracePeriod;
    }

    public void scan() {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, clock.getZone());
        for (OpenOccurrenceRef ref : occurrenceQueries.findOpenOnOrBefore(today)) {
            // A per-Occurrence end time (a reschedule, or a monthly-ad-hoc
            // Occurrence's own slot) wins; otherwise fall back to the Sabha's
            // standing weekly schedule. Monthly Sabhas have no standing schedule,
            // so an Occurrence with neither is skipped.
            LocalTime endTime = ref.rescheduledEndTime();
            if (endTime == null) {
                Optional<SabhaSchedule> schedule = scheduleLookup.findSchedule(ref.sabhaId());
                if (schedule.isEmpty()) {
                    continue;
                }
                endTime = schedule.get().endTime();
            }
            Instant scheduledEndAt = ZonedDateTime.of(
                    ref.date(), endTime, clock.getZone()).toInstant();
            Instant cutoff = scheduledEndAt.plus(gracePeriod);
            if (!cutoff.isAfter(now)) {
                writer.transition(ref.occurrenceId(), TransitionActor.system(),
                        OccurrenceAction.FINALIZE, null, Occurrence::markFinalized);
            }
        }
    }
}
