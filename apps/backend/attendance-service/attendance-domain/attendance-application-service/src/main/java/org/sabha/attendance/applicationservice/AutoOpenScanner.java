package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Instant;

import org.sabha.attendance.domain.Occurrence;
import org.springframework.stereotype.Component;

/**
 * Auto-Open scanner (Slice 3 / PRD-0001). Driven by Spring scheduling on a
 * minutely cadence: finds Scheduled Occurrences whose Effective Slot has started
 * and opens them through the single Occurrence write path.
 *
 * <p>Where that slot falls in absolute time — per-Occurrence override over the
 * Sabha's standing schedule, and the timezone arithmetic — is
 * {@link EffectiveSlotResolver}'s call. An Occurrence with no resolvable slot is
 * skipped.</p>
 */
@Component
public class AutoOpenScanner {

    private final OccurrenceQueries occurrenceQueries;
    private final EffectiveSlotResolver slotResolver;
    private final OccurrenceWriter writer;
    private final Clock clock;

    public AutoOpenScanner(
            OccurrenceQueries occurrenceQueries,
            EffectiveSlotResolver slotResolver,
            OccurrenceWriter writer,
            Clock clock) {
        this.occurrenceQueries = occurrenceQueries;
        this.slotResolver = slotResolver;
        this.writer = writer;
        this.clock = clock;
    }

    public void scan() {
        Instant now = clock.instant();
        for (OccurrenceSlotRef ref : occurrenceQueries.findScheduledOnOrBefore(slotResolver.today())) {
            slotResolver.resolve(ref)
                    .filter(slot -> !slot.startsAt().isAfter(now))
                    .ifPresent(slot -> writer.transition(ref.occurrenceId(), TransitionActor.system(),
                            OccurrenceAction.OPEN, null, Occurrence::open));
        }
    }
}
