package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
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
import org.sabha.attendance.domain.OccurrenceFinalized;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.common.SabhaSchedule;
import org.sabha.common.SabhaScheduleLookup;

import static org.assertj.core.api.Assertions.assertThat;

class AutoFinalizeScannerTest {

    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");
    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID DUE_OCCURRENCE = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID NOT_YET_DUE_OCCURRENCE = UUID.fromString("00000000-0000-0000-0000-000000000021");

    @Test
    void finalizesOnlyOpenOccurrencesWhoseScheduledEndPlus24hHasPassed() {
        // Occurrence date 2026-05-25 ends 20:00 IST. Cutoff = 2026-05-26 20:00 IST.
        // Now = 2026-05-26 20:30 IST — DUE_OCCURRENCE is past cutoff.
        // NOT_YET_DUE_OCCURRENCE is dated 2026-05-26 ending 20:00 IST (cutoff 2026-05-27 20:00) — not due.
        Instant now = LocalDate.of(2026, 5, 26).atTime(20, 30).atZone(KOLKATA).toInstant();
        Clock clock = Clock.fixed(now, KOLKATA);

        StubOccurrenceQueries queries = new StubOccurrenceQueries();
        queries.open.add(new OpenOccurrenceRef(DUE_OCCURRENCE, SABHA_ID,
                LocalDate.of(2026, 5, 25)));
        queries.open.add(new OpenOccurrenceRef(NOT_YET_DUE_OCCURRENCE, SABHA_ID,
                LocalDate.of(2026, 5, 26)));

        StubSabhaScheduleLookup lookup = new StubSabhaScheduleLookup();
        lookup.put(SABHA_ID, new SabhaSchedule(DayOfWeek.SUNDAY,
                LocalTime.of(19, 0), LocalTime.of(20, 0)));

        OccurrenceWriterTest.RecordingOccurrenceRepository occurrences =
                new OccurrenceWriterTest.RecordingOccurrenceRepository();
        occurrences.put(new Occurrence(DUE_OCCURRENCE, SABHA_ID,
                LocalDate.of(2026, 5, 25), OccurrenceState.OPEN_FOR_MARKING));
        occurrences.put(new Occurrence(NOT_YET_DUE_OCCURRENCE, SABHA_ID,
                LocalDate.of(2026, 5, 26), OccurrenceState.OPEN_FOR_MARKING));
        OccurrenceWriterTest.InMemoryTransitionLog log =
                new OccurrenceWriterTest.InMemoryTransitionLog();
        OccurrenceWriterTest.CapturingPublisher publisher =
                new OccurrenceWriterTest.CapturingPublisher();
        OccurrenceWriter writer = OccurrenceWriterTest.cronWriter(
                occurrences, log, publisher, clock);

        AutoFinalizeScanner scanner = new AutoFinalizeScanner(
                queries, lookup, writer, clock, Duration.ofHours(24));

        scanner.scan();

        assertThat(occurrences.savedOccurrences()).hasSize(1);
        assertThat(occurrences.savedOccurrences().get(0).id()).isEqualTo(DUE_OCCURRENCE);
        assertThat(occurrences.savedOccurrences().get(0).state())
                .isEqualTo(OccurrenceState.FINALIZED);

        assertThat(log.appended).hasSize(1);
        OccurrenceStateTransition row = log.appended.get(0);
        assertThat(row.occurrenceId()).isEqualTo(DUE_OCCURRENCE);
        assertThat(row.action()).isEqualTo(OccurrenceAction.FINALIZE);
        assertThat(row.actorKind()).isEqualTo(ActorKind.SYSTEM);

        assertThat(publisher.published).singleElement().isInstanceOf(OccurrenceFinalized.class);
    }

    @Test
    void finalizesARescheduledOccurrenceByItsRescheduledEndTimeNotTheStandingSchedule() {
        // Standing slot ends 20:00. The occurrence (dated 2026-05-25) was rescheduled to
        // end at 22:00, so its cutoff is 2026-05-26 22:00 + 24h = 2026-05-27 22:00 IST.
        // now = 2026-05-27 21:00 IST — past the standing-schedule cutoff but before the
        // rescheduled one, so it must NOT finalize.
        Instant now = LocalDate.of(2026, 5, 27).atTime(21, 0).atZone(KOLKATA).toInstant();
        Clock clock = Clock.fixed(now, KOLKATA);

        StubOccurrenceQueries queries = new StubOccurrenceQueries();
        queries.open.add(new OpenOccurrenceRef(DUE_OCCURRENCE, SABHA_ID,
                LocalDate.of(2026, 5, 26), LocalTime.of(22, 0)));

        StubSabhaScheduleLookup lookup = new StubSabhaScheduleLookup();
        lookup.put(SABHA_ID, new SabhaSchedule(DayOfWeek.SUNDAY,
                LocalTime.of(19, 0), LocalTime.of(20, 0)));

        OccurrenceWriterTest.RecordingOccurrenceRepository occurrences =
                new OccurrenceWriterTest.RecordingOccurrenceRepository();
        occurrences.put(new Occurrence(DUE_OCCURRENCE, SABHA_ID,
                LocalDate.of(2026, 5, 26), OccurrenceState.OPEN_FOR_MARKING));
        OccurrenceWriterTest.InMemoryTransitionLog log =
                new OccurrenceWriterTest.InMemoryTransitionLog();
        OccurrenceWriterTest.CapturingPublisher publisher =
                new OccurrenceWriterTest.CapturingPublisher();
        OccurrenceWriter writer = OccurrenceWriterTest.cronWriter(
                occurrences, log, publisher, clock);

        AutoFinalizeScanner scanner = new AutoFinalizeScanner(
                queries, lookup, writer, clock, Duration.ofHours(24));

        scanner.scan();

        assertThat(occurrences.savedOccurrences()).isEmpty();
        assertThat(log.appended).isEmpty();
    }

    @Test
    void finalizesAMonthlyAdHocOccurrenceByItsOwnEndTimeWhenTheSabhaHasNoStandingSchedule() {
        // Monthly Sabha, no standing schedule. Occurrence dated 2026-06-21 ends 10:30;
        // cutoff = 2026-06-22 10:30 IST. now = 2026-06-22 11:00 IST — past cutoff.
        Instant now = LocalDate.of(2026, 6, 22).atTime(11, 0).atZone(KOLKATA).toInstant();
        Clock clock = Clock.fixed(now, KOLKATA);
        UUID monthlySabha = UUID.fromString("00000000-0000-0000-0000-0000000000a9");
        UUID monthlyOccurrence = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

        StubOccurrenceQueries queries = new StubOccurrenceQueries();
        queries.open.add(new OpenOccurrenceRef(monthlyOccurrence, monthlySabha,
                LocalDate.of(2026, 6, 21), LocalTime.of(10, 30)));

        StubSabhaScheduleLookup lookup = new StubSabhaScheduleLookup(); // no schedule for the monthly Sabha

        OccurrenceWriterTest.RecordingOccurrenceRepository occurrences =
                new OccurrenceWriterTest.RecordingOccurrenceRepository();
        occurrences.put(new Occurrence(monthlyOccurrence, monthlySabha,
                LocalDate.of(2026, 6, 21), OccurrenceState.OPEN_FOR_MARKING));
        OccurrenceWriterTest.InMemoryTransitionLog log =
                new OccurrenceWriterTest.InMemoryTransitionLog();
        OccurrenceWriterTest.CapturingPublisher publisher =
                new OccurrenceWriterTest.CapturingPublisher();
        OccurrenceWriter writer = OccurrenceWriterTest.cronWriter(occurrences, log, publisher, clock);

        new AutoFinalizeScanner(queries, lookup, writer, clock, Duration.ofHours(24)).scan();

        assertThat(occurrences.savedOccurrences()).hasSize(1);
        assertThat(occurrences.savedOccurrences().get(0).id()).isEqualTo(monthlyOccurrence);
        assertThat(occurrences.savedOccurrences().get(0).state()).isEqualTo(OccurrenceState.FINALIZED);
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
