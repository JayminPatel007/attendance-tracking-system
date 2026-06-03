package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.common.SabhaSchedule;
import org.sabha.common.WeeklySabhaCatalog;
import org.sabha.common.WeeklySabhaRef;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyMaterializationScannerTest {

    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");
    private static final UUID SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final SabhaSchedule SUNDAY_9AM =
            new SabhaSchedule(DayOfWeek.SUNDAY, LocalTime.of(9, 0), LocalTime.of(10, 30));

    private final FakeCatalog catalog = new FakeCatalog();
    private final FakeCalendar calendar = new FakeCalendar();
    private final RecordingOccurrences occurrences = new RecordingOccurrences();

    @Test
    void materializesAnEightWeekWindowStartingTheNextSlotAtLeast24hOut() {
        // Wed 2026-06-03 12:00 IST — the next Sunday slot (06-07) is ~4 days out.
        Clock clock = fixedAt(LocalDate.of(2026, 6, 3), LocalTime.of(12, 0));
        catalog.put(SABHA, SUNDAY_9AM);

        scanner(clock).scan();

        List<LocalDate> dates = occurrences.scheduledDatesFor(SABHA);
        assertThat(dates).hasSize(8);
        assertThat(dates.get(0)).isEqualTo(LocalDate.of(2026, 6, 7));
        assertThat(dates).allSatisfy(d -> assertThat(d.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY));
        assertThat(occurrences.saved).allMatch(o -> o.state() == OccurrenceState.SCHEDULED);
    }

    @Test
    void skipsToTheFollowingWeekWhenTheNextSlotIsSoonerThan24h() {
        // Sat 2026-06-06 20:00 IST — the next Sunday slot (06-07 09:00) is ~13h out.
        Clock clock = fixedAt(LocalDate.of(2026, 6, 6), LocalTime.of(20, 0));
        catalog.put(SABHA, SUNDAY_9AM);

        scanner(clock).scan();

        assertThat(occurrences.scheduledDatesFor(SABHA).get(0)).isEqualTo(LocalDate.of(2026, 6, 14));
    }

    @Test
    void doesNotDuplicateOccurrencesThatAlreadyExist() {
        Clock clock = fixedAt(LocalDate.of(2026, 6, 3), LocalTime.of(12, 0));
        catalog.put(SABHA, SUNDAY_9AM);
        calendar.markExisting(SABHA, LocalDate.of(2026, 6, 7));
        calendar.markExisting(SABHA, LocalDate.of(2026, 6, 21));

        scanner(clock).scan();

        List<LocalDate> dates = occurrences.scheduledDatesFor(SABHA);
        assertThat(dates).doesNotContain(LocalDate.of(2026, 6, 7), LocalDate.of(2026, 6, 21));
        assertThat(dates).hasSize(6);
    }

    private WeeklyMaterializationScanner scanner(Clock clock) {
        return new WeeklyMaterializationScanner(catalog, calendar, occurrences, clock);
    }

    private static Clock fixedAt(LocalDate date, LocalTime time) {
        return Clock.fixed(date.atTime(time).atZone(KOLKATA).toInstant(), KOLKATA);
    }

    private static final class FakeCatalog implements WeeklySabhaCatalog {
        final List<WeeklySabhaRef> refs = new ArrayList<>();

        void put(UUID sabhaId, SabhaSchedule schedule) {
            refs.add(new WeeklySabhaRef(sabhaId, schedule));
        }

        @Override
        public List<WeeklySabhaRef> findAllWeekly() {
            return refs;
        }
    }

    private static final class FakeCalendar implements OccurrenceCalendar {
        final Set<String> existing = new HashSet<>();

        void markExisting(UUID sabhaId, LocalDate date) {
            existing.add(sabhaId + "@" + date);
        }

        @Override
        public boolean exists(UUID sabhaId, LocalDate date) {
            return existing.contains(sabhaId + "@" + date);
        }

        @Override
        public boolean existsInMonth(UUID sabhaId, java.time.YearMonth month) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingOccurrences implements OccurrenceInsert {
        final List<Occurrence> saved = new ArrayList<>();

        List<LocalDate> scheduledDatesFor(UUID sabhaId) {
            return saved.stream().filter(o -> o.sabhaId().equals(sabhaId)).map(Occurrence::date).toList();
        }

        @Override
        public void add(Occurrence occurrence) {
            saved.add(occurrence);
        }
    }
}
