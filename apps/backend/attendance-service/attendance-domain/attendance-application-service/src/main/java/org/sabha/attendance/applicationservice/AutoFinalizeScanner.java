package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.sabha.common.SabhaSchedule;
import org.sabha.common.SabhaScheduleLookup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Auto-Finalize scanner (Slice 3 / PRD-0001). Driven by Spring scheduling on
 * an hourly cadence: finds Open-for-Marking Occurrences whose scheduled end
 * Instant plus a grace period (24h per ADR-0001) has passed, and dispatches
 * the FINALIZE action through the state machine.
 */
@Component
public class AutoFinalizeScanner {

    private final OccurrenceQueries occurrenceQueries;
    private final SabhaScheduleLookup scheduleLookup;
    private final OccurrenceStateMachine stateMachine;
    private final Clock clock;
    private final Duration gracePeriod;

    public AutoFinalizeScanner(
            OccurrenceQueries occurrenceQueries,
            SabhaScheduleLookup scheduleLookup,
            OccurrenceStateMachine stateMachine,
            Clock clock,
            @Value("${sabha.attendance.auto-finalize.grace:PT24H}") Duration gracePeriod) {
        this.occurrenceQueries = occurrenceQueries;
        this.scheduleLookup = scheduleLookup;
        this.stateMachine = stateMachine;
        this.clock = clock;
        this.gracePeriod = gracePeriod;
    }

    public void scan() {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, clock.getZone());
        for (OpenOccurrenceRef ref : occurrenceQueries.findOpenOnOrBefore(today)) {
            Optional<SabhaSchedule> schedule = scheduleLookup.findSchedule(ref.sabhaId());
            if (schedule.isEmpty()) {
                continue;
            }
            LocalTime endTime = ref.rescheduledEndTime() != null
                    ? ref.rescheduledEndTime()
                    : schedule.get().endTime();
            Instant scheduledEndAt = ZonedDateTime.of(
                    ref.date(), endTime, clock.getZone()).toInstant();
            Instant cutoff = scheduledEndAt.plus(gracePeriod);
            if (!cutoff.isAfter(now)) {
                stateMachine.transition(ref.occurrenceId(),
                        OccurrenceAction.FINALIZE, TransitionActor.system());
            }
        }
    }
}
