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
 * Resolves an Occurrence to its Effective Slot (see CONTEXT.md): the absolute
 * instants it starts and ends at.
 *
 * <p>Cross-context schedule resolution goes through {@link SabhaScheduleLookup}
 * (ADR-0019).</p>
 */
@Component
public class EffectiveSlotResolver {

    private final SabhaScheduleLookup scheduleLookup;
    private final Clock clock;

    public EffectiveSlotResolver(SabhaScheduleLookup scheduleLookup, Clock clock) {
        this.scheduleLookup = scheduleLookup;
        this.clock = clock;
    }

    /** Today's date in the scheduling zone — the zone Effective Slots are read in. */
    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), clock.getZone());
    }

    /**
     * Resolves a loaded Occurrence aggregate. Its rescheduled date/time — set by
     * a reschedule, or by a monthly-ad-hoc Occurrence's own picked slot
     * (ADR-0012) — take precedence exactly as they do for the cron scanners.
     */
    public Optional<EffectiveSlot> resolve(Occurrence occurrence) {
        LocalDate effectiveDate = occurrence.rescheduledDate() != null
                ? occurrence.rescheduledDate()
                : occurrence.date();
        return resolve(new OccurrenceSlotRef(occurrence.id(), occurrence.sabhaId(), effectiveDate,
                occurrence.rescheduledStartTime(), occurrence.rescheduledEndTime()));
    }

    /**
     * A per-Occurrence override (a reschedule, or a monthly-ad-hoc Occurrence's
     * own slot) wins boundary by boundary; each boundary the Occurrence does not
     * override falls back to the Sabha's standing weekly schedule. Monthly Sabhas
     * have no standing schedule, so an Occurrence leaving a boundary unresolved
     * has no Effective Slot at all — callers skip it.
     */
    public Optional<EffectiveSlot> resolve(OccurrenceSlotRef ref) {
        Optional<SabhaSchedule> standing = ref.overrideStartTime() == null || ref.overrideEndTime() == null
                ? scheduleLookup.findSchedule(ref.sabhaId())
                : Optional.empty();
        LocalTime startTime = boundary(ref.overrideStartTime(), standing.map(SabhaSchedule::startTime));
        LocalTime endTime = boundary(ref.overrideEndTime(), standing.map(SabhaSchedule::endTime));
        if (startTime == null || endTime == null) {
            return Optional.empty();
        }
        return Optional.of(new EffectiveSlot(at(ref.date(), startTime), at(ref.date(), endTime)));
    }

    private static LocalTime boundary(LocalTime override, Optional<LocalTime> standing) {
        return override != null ? override : standing.orElse(null);
    }

    private Instant at(LocalDate date, LocalTime time) {
        return ZonedDateTime.of(date, time, clock.getZone()).toInstant();
    }
}
