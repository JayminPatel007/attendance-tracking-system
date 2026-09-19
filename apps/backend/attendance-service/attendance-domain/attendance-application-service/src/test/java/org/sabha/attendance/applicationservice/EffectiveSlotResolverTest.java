package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.attendance.domain.Occurrence;
import org.sabha.common.SabhaSchedule;
import org.sabha.common.SabhaFact;
import org.sabha.common.SabhaFacts;
import org.sabha.common.SabhaScope;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;

class EffectiveSlotResolverTest {

    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");
    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @Test
    void fallsBackToTheSabhaStandingScheduleWhenTheOccurrenceCarriesNoOverride() {
        StubSabhaFacts lookup = new StubSabhaFacts();
        lookup.put(SABHA_ID, new SabhaSchedule(DayOfWeek.TUESDAY,
                LocalTime.of(19, 0), LocalTime.of(20, 0)));
        EffectiveSlotResolver resolver = new EffectiveSlotResolver(lookup, clockAt(KOLKATA));

        EffectiveSlot slot = resolver.resolve(new OccurrenceSlotRef(
                OCCURRENCE_ID, SABHA_ID, LocalDate.of(2026, 5, 26))).orElseThrow();

        // Asia/Kolkata is UTC+05:30, so 19:00–20:00 local is 13:30–14:30 UTC.
        assertThat(slot.startsAt()).isEqualTo(Instant.parse("2026-05-26T13:30:00Z"));
        assertThat(slot.endsAt()).isEqualTo(Instant.parse("2026-05-26T14:30:00Z"));
    }

    @Test
    void prefersThePerOccurrenceOverrideOverTheSabhaStandingSchedule() {
        StubSabhaFacts lookup = new StubSabhaFacts();
        lookup.put(SABHA_ID, new SabhaSchedule(DayOfWeek.TUESDAY,
                LocalTime.of(19, 0), LocalTime.of(20, 0)));
        EffectiveSlotResolver resolver = new EffectiveSlotResolver(lookup, clockAt(KOLKATA));

        EffectiveSlot slot = resolver.resolve(new OccurrenceSlotRef(
                OCCURRENCE_ID, SABHA_ID, LocalDate.of(2026, 5, 26),
                LocalTime.of(19, 30), LocalTime.of(22, 0))).orElseThrow();

        // The rescheduled 19:30–22:00 IST slot, not the standing 19:00–20:00 one.
        assertThat(slot.startsAt()).isEqualTo(Instant.parse("2026-05-26T14:00:00Z"));
        assertThat(slot.endsAt()).isEqualTo(Instant.parse("2026-05-26T16:30:00Z"));
    }

    @Test
    void appliesOverridePrecedenceOneBoundaryAtATime() {
        StubSabhaFacts lookup = new StubSabhaFacts();
        lookup.put(SABHA_ID, new SabhaSchedule(DayOfWeek.TUESDAY,
                LocalTime.of(19, 0), LocalTime.of(20, 0)));
        EffectiveSlotResolver resolver = new EffectiveSlotResolver(lookup, clockAt(KOLKATA));

        EffectiveSlot slot = resolver.resolve(new OccurrenceSlotRef(
                OCCURRENCE_ID, SABHA_ID, LocalDate.of(2026, 5, 26),
                LocalTime.of(19, 30), null)).orElseThrow();

        // Overridden start, standing end: 19:30–20:00 IST.
        assertThat(slot.startsAt()).isEqualTo(Instant.parse("2026-05-26T14:00:00Z"));
        assertThat(slot.endsAt()).isEqualTo(Instant.parse("2026-05-26T14:30:00Z"));
    }

    @Test
    void resolvesNothingWhenTheSabhaHasNoStandingScheduleAndTheOccurrenceCarriesNoOverride() {
        // A monthly-ad-hoc Sabha has no standing schedule to fall back to.
        EffectiveSlotResolver resolver = new EffectiveSlotResolver(
                new StubSabhaFacts(), clockAt(KOLKATA));

        Optional<EffectiveSlot> slot = resolver.resolve(new OccurrenceSlotRef(
                OCCURRENCE_ID, SABHA_ID, LocalDate.of(2026, 5, 26)));

        assertThat(slot).isEmpty();
    }

    @Test
    void resolvesARescheduledOccurrenceAggregateOnItsRescheduledDateAndTime() {
        StubSabhaFacts lookup = new StubSabhaFacts();
        lookup.put(SABHA_ID, new SabhaSchedule(DayOfWeek.TUESDAY,
                LocalTime.of(19, 0), LocalTime.of(20, 0)));
        EffectiveSlotResolver resolver = new EffectiveSlotResolver(lookup, clockAt(KOLKATA));

        Occurrence occurrence = Occurrence.scheduled(OCCURRENCE_ID, SABHA_ID, LocalDate.of(2026, 5, 26));
        occurrence.reschedule(LocalDate.of(2026, 5, 31), LocalTime.of(9, 0), LocalTime.of(10, 30));

        EffectiveSlot slot = resolver.resolve(occurrence).orElseThrow();

        // The rescheduled 2026-05-31 09:00–10:30 IST, not 2026-05-26 19:00–20:00.
        assertThat(slot.startsAt()).isEqualTo(Instant.parse("2026-05-31T03:30:00Z"));
        assertThat(slot.endsAt()).isEqualTo(Instant.parse("2026-05-31T05:00:00Z"));
    }

    @Test
    void reportsTodayInTheSchedulingZoneNotInUtc() {
        // 2026-05-26 20:00 UTC is already 2026-05-27 in Asia/Kolkata (UTC+05:30).
        Clock clock = Clock.fixed(Instant.parse("2026-05-26T20:00:00Z"), KOLKATA);
        EffectiveSlotResolver resolver = new EffectiveSlotResolver(new StubSabhaFacts(), clock);

        assertThat(resolver.today()).isEqualTo(LocalDate.of(2026, 5, 27));
    }

    private static Clock clockAt(ZoneId zone) {
        return Clock.fixed(Instant.parse("2026-05-26T00:00:00Z"), zone);
    }

    /**
     * Only the standing slot matters on these paths, so the stub seeds schedules and
     * lets {@link SabhaFact#weekly} hold the shape/slot invariant. A Sabha with no
     * seeded schedule is simply unknown, which is what a monthly-ad-hoc one looked
     * like through the old schedule-only port.
     */
    private static final class StubSabhaFacts implements SabhaFacts {
        static final SabhaScope ANY_SCOPE = new SabhaScope(null, null, null);

        final Map<UUID, SabhaSchedule> schedules = new HashMap<>();

        void put(UUID sabhaId, SabhaSchedule schedule) {
            schedules.put(sabhaId, schedule);
        }

        @Override
        public Optional<SabhaFact> of(UUID sabhaId) {
            return Optional.ofNullable(schedules.get(sabhaId))
                    .map(schedule -> SabhaFact.weekly(sabhaId, ANY_SCOPE, schedule, false));
        }

        @Override
        public List<SabhaFact> allWeekly() {
            return schedules.entrySet().stream()
                    .map(e -> SabhaFact.weekly(e.getKey(), ANY_SCOPE, e.getValue(), false))
                    .toList();
        }

        @Override
        public Optional<SabhaFact> selectiveIn(UUID kshetraId, String demographic, String track) {
            return Optional.empty();
        }
    }
}
