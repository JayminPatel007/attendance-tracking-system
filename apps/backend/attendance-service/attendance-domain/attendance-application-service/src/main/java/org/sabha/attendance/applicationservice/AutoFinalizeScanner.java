package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.sabha.attendance.domain.Occurrence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Auto-Finalize scanner (Slice 3 / PRD-0001). Driven by Spring scheduling on
 * an hourly cadence: finds Open-for-Marking Occurrences whose Effective Slot
 * ended more than a grace period ago (24h per ADR-0001), and finalizes them
 * through the single Occurrence write path.
 *
 * <p>Where that slot falls in absolute time is {@link EffectiveSlotResolver}'s
 * call; an Occurrence with no resolvable slot is skipped.</p>
 */
@Component
public class AutoFinalizeScanner {

    private final OccurrenceQueries occurrenceQueries;
    private final EffectiveSlotResolver slotResolver;
    private final OccurrenceWriter writer;
    private final Clock clock;
    private final Duration gracePeriod;

    public AutoFinalizeScanner(
            OccurrenceQueries occurrenceQueries,
            EffectiveSlotResolver slotResolver,
            OccurrenceWriter writer,
            Clock clock,
            @Value("${sabha.attendance.auto-finalize.grace:PT24H}") Duration gracePeriod) {
        this.occurrenceQueries = occurrenceQueries;
        this.slotResolver = slotResolver;
        this.writer = writer;
        this.clock = clock;
        this.gracePeriod = gracePeriod;
    }

    public void scan() {
        Instant now = clock.instant();
        for (OccurrenceSlotRef ref : occurrenceQueries.findOpenOnOrBefore(slotResolver.today())) {
            slotResolver.resolve(ref)
                    .filter(slot -> !slot.endsAt().plus(gracePeriod).isAfter(now))
                    .ifPresent(slot -> writer.transition(ref.occurrenceId(), TransitionActor.system(),
                            OccurrenceAction.FINALIZE, null, Occurrence::markFinalized));
        }
    }
}
