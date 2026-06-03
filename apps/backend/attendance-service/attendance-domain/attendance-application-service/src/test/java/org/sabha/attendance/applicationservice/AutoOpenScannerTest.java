package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceOpened;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.common.DomainEvent;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.SabhaSchedule;
import org.sabha.common.SabhaScheduleLookup;

import static org.assertj.core.api.Assertions.assertThat;

class AutoOpenScannerTest {

    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");
    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID DUE_OCCURRENCE = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID NOT_YET_DUE_OCCURRENCE = UUID.fromString("00000000-0000-0000-0000-000000000021");

    @Test
    void opensOnlyScheduledOccurrencesWhoseStartTimeHasPassed() {
        // 2026-05-26 19:30 Asia/Kolkata — past the 19:00 Sunday slot for DUE_OCCURRENCE,
        // but before the 21:00 slot for NOT_YET_DUE_OCCURRENCE.
        Instant now = LocalDate.of(2026, 5, 26).atTime(19, 30).atZone(KOLKATA).toInstant();
        Clock clock = Clock.fixed(now, KOLKATA);
        UUID otherSabhaId = UUID.fromString("00000000-0000-0000-0000-000000000007");

        StubOccurrenceQueries queries = new StubOccurrenceQueries();
        queries.scheduled.add(new ScheduledOccurrenceRef(DUE_OCCURRENCE, SABHA_ID,
                LocalDate.of(2026, 5, 26)));
        queries.scheduled.add(new ScheduledOccurrenceRef(NOT_YET_DUE_OCCURRENCE, otherSabhaId,
                LocalDate.of(2026, 5, 26)));

        StubSabhaScheduleLookup lookup = new StubSabhaScheduleLookup();
        lookup.put(SABHA_ID, new SabhaSchedule(DayOfWeek.TUESDAY,
                LocalTime.of(19, 0), LocalTime.of(20, 0)));
        lookup.put(otherSabhaId, new SabhaSchedule(DayOfWeek.TUESDAY,
                LocalTime.of(21, 0), LocalTime.of(22, 0)));

        OccurrenceStateMachineTest.InMemoryOccurrenceRepository occurrences =
                new OccurrenceStateMachineTest.InMemoryOccurrenceRepository();
        occurrences.put(Occurrence.scheduled(DUE_OCCURRENCE, SABHA_ID, LocalDate.of(2026, 5, 26)));
        occurrences.put(Occurrence.scheduled(NOT_YET_DUE_OCCURRENCE, otherSabhaId, LocalDate.of(2026, 5, 26)));
        OccurrenceStateMachineTest.InMemoryTransitionLog log =
                new OccurrenceStateMachineTest.InMemoryTransitionLog();
        OccurrenceStateMachineTest.CapturingPublisher publisher =
                new OccurrenceStateMachineTest.CapturingPublisher();
        OccurrenceStateMachine stateMachine = new OccurrenceStateMachine(
                occurrences, log, publisher, clock);

        AutoOpenScanner scanner = new AutoOpenScanner(queries, lookup, stateMachine, clock);

        scanner.scan();

        assertThat(occurrences.savedOccurrences()).hasSize(1);
        assertThat(occurrences.savedOccurrences().get(0).id()).isEqualTo(DUE_OCCURRENCE);
        assertThat(occurrences.savedOccurrences().get(0).state())
                .isEqualTo(OccurrenceState.OPEN_FOR_MARKING);

        assertThat(log.appended).hasSize(1);
        OccurrenceStateTransition row = log.appended.get(0);
        assertThat(row.occurrenceId()).isEqualTo(DUE_OCCURRENCE);
        assertThat(row.action()).isEqualTo(OccurrenceAction.OPEN);
        assertThat(row.actorKind()).isEqualTo(ActorKind.SYSTEM);

        assertThat(publisher.published).singleElement().isInstanceOf(OccurrenceOpened.class);
    }

    @Test
    void opensARescheduledOccurrenceByItsRescheduledStartTimeNotTheStandingSchedule() {
        // Standing slot is 19:00. now = 20:00 IST on the rescheduled date. The
        // occurrence rescheduled to 19:30 is due; the one rescheduled to 21:00 is not —
        // proving the scanner honours the per-Occurrence override, not the 19:00 standing slot.
        Instant now = LocalDate.of(2026, 5, 31).atTime(20, 0).atZone(KOLKATA).toInstant();
        Clock clock = Clock.fixed(now, KOLKATA);
        UUID dueRescheduled = UUID.fromString("00000000-0000-0000-0000-000000000030");
        UUID notYetRescheduled = UUID.fromString("00000000-0000-0000-0000-000000000031");

        StubOccurrenceQueries queries = new StubOccurrenceQueries();
        queries.scheduled.add(new ScheduledOccurrenceRef(dueRescheduled, SABHA_ID,
                LocalDate.of(2026, 5, 31), LocalTime.of(19, 30)));
        queries.scheduled.add(new ScheduledOccurrenceRef(notYetRescheduled, SABHA_ID,
                LocalDate.of(2026, 5, 31), LocalTime.of(21, 0)));

        StubSabhaScheduleLookup lookup = new StubSabhaScheduleLookup();
        lookup.put(SABHA_ID, new SabhaSchedule(DayOfWeek.SUNDAY,
                LocalTime.of(19, 0), LocalTime.of(20, 0)));

        OccurrenceStateMachineTest.InMemoryOccurrenceRepository occurrences =
                new OccurrenceStateMachineTest.InMemoryOccurrenceRepository();
        occurrences.put(new Occurrence(dueRescheduled, SABHA_ID,
                LocalDate.of(2026, 5, 24), OccurrenceState.RESCHEDULED));
        occurrences.put(new Occurrence(notYetRescheduled, SABHA_ID,
                LocalDate.of(2026, 5, 24), OccurrenceState.RESCHEDULED));
        OccurrenceStateMachineTest.InMemoryTransitionLog log =
                new OccurrenceStateMachineTest.InMemoryTransitionLog();
        OccurrenceStateMachineTest.CapturingPublisher publisher =
                new OccurrenceStateMachineTest.CapturingPublisher();
        OccurrenceStateMachine stateMachine = new OccurrenceStateMachine(
                occurrences, log, publisher, clock);

        AutoOpenScanner scanner = new AutoOpenScanner(queries, lookup, stateMachine, clock);

        scanner.scan();

        assertThat(occurrences.savedOccurrences()).hasSize(1);
        assertThat(occurrences.savedOccurrences().get(0).id()).isEqualTo(dueRescheduled);
        assertThat(occurrences.savedOccurrences().get(0).state())
                .isEqualTo(OccurrenceState.OPEN_FOR_MARKING);
    }

    @Test
    void opensAMonthlyAdHocOccurrenceByItsOwnTimeWhenTheSabhaHasNoStandingSchedule() {
        // A monthly-ad-hoc Sabha has no standing schedule; the Occurrence carries its
        // own start time. now = 2026-06-21 10:00 IST, the occurrence's 09:00 slot passed.
        Instant now = LocalDate.of(2026, 6, 21).atTime(10, 0).atZone(KOLKATA).toInstant();
        Clock clock = Clock.fixed(now, KOLKATA);
        UUID monthlySabha = UUID.fromString("00000000-0000-0000-0000-0000000000a9");
        UUID monthlyOccurrence = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

        StubOccurrenceQueries queries = new StubOccurrenceQueries();
        queries.scheduled.add(new ScheduledOccurrenceRef(monthlyOccurrence, monthlySabha,
                LocalDate.of(2026, 6, 21), LocalTime.of(9, 0)));

        StubSabhaScheduleLookup lookup = new StubSabhaScheduleLookup(); // no schedule for the monthly Sabha

        OccurrenceStateMachineTest.InMemoryOccurrenceRepository occurrences =
                new OccurrenceStateMachineTest.InMemoryOccurrenceRepository();
        occurrences.put(Occurrence.scheduled(monthlyOccurrence, monthlySabha, LocalDate.of(2026, 6, 21)));
        OccurrenceStateMachineTest.InMemoryTransitionLog log =
                new OccurrenceStateMachineTest.InMemoryTransitionLog();
        OccurrenceStateMachineTest.CapturingPublisher publisher =
                new OccurrenceStateMachineTest.CapturingPublisher();
        OccurrenceStateMachine stateMachine = new OccurrenceStateMachine(occurrences, log, publisher, clock);

        new AutoOpenScanner(queries, lookup, stateMachine, clock).scan();

        assertThat(occurrences.savedOccurrences()).hasSize(1);
        assertThat(occurrences.savedOccurrences().get(0).id()).isEqualTo(monthlyOccurrence);
        assertThat(occurrences.savedOccurrences().get(0).state()).isEqualTo(OccurrenceState.OPEN_FOR_MARKING);
    }

    private static final class StubOccurrenceQueries implements OccurrenceQueries {
        final List<ScheduledOccurrenceRef> scheduled = new ArrayList<>();
        final List<OpenOccurrenceRef> open = new ArrayList<>();

        @Override
        public List<ScheduledOccurrenceRef> findScheduledOnOrBefore(LocalDate date) {
            return scheduled;
        }

        @Override
        public List<OpenOccurrenceRef> findOpenOnOrBefore(LocalDate date) {
            return open;
        }
    }

    private static final class StubSabhaScheduleLookup implements SabhaScheduleLookup {
        final Map<UUID, SabhaSchedule> schedules = new HashMap<>();

        void put(UUID sabhaId, SabhaSchedule schedule) {
            schedules.put(sabhaId, schedule);
        }

        @Override
        public Optional<SabhaSchedule> findSchedule(UUID sabhaId) {
            return Optional.ofNullable(schedules.get(sabhaId));
        }
    }
}
