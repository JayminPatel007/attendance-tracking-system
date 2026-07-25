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
import org.sabha.common.SabhaScheduleLookup;

import static org.assertj.core.api.Assertions.assertThat;

class EffectiveSlotResolverTest {

    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");
    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @Test
    void fallsBackToTheSabhaStandingScheduleWhenTheOccurrenceCarriesNoOverride() {
        StubSabhaScheduleLookup lookup = new StubSabhaScheduleLookup();
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
        StubSabhaScheduleLookup lookup = new StubSabhaScheduleLookup();
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
        StubSabhaScheduleLookup lookup = new StubSabhaScheduleLookup();
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
                new StubSabhaScheduleLookup(), clockAt(KOLKATA));

        Optional<EffectiveSlot> slot = resolver.resolve(new OccurrenceSlotRef(
                OCCURRENCE_ID, SABHA_ID, LocalDate.of(2026, 5, 26)));

        assertThat(slot).isEmpty();
    }

    @Test
    void resolvesARescheduledOccurrenceAggregateOnItsRescheduledDateAndTime() {
        StubSabhaScheduleLookup lookup = new StubSabhaScheduleLookup();
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
        EffectiveSlotResolver resolver = new EffectiveSlotResolver(new StubSabhaScheduleLookup(), clock);

        assertThat(resolver.today()).isEqualTo(LocalDate.of(2026, 5, 27));
    }

    private static Clock clockAt(ZoneId zone) {
        return Clock.fixed(Instant.parse("2026-05-26T00:00:00Z"), zone);
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
