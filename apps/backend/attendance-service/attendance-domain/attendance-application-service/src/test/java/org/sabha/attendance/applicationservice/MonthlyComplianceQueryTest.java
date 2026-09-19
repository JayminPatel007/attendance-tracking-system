package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.SabhaFact;
import org.sabha.common.SabhaFacts;
import org.sabha.common.SabhaScope;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.time.DayOfWeek;
import java.time.LocalTime;
import org.sabha.common.SabhaSchedule;

class MonthlyComplianceQueryTest {

    private static final UUID MONTHLY_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID WEEKLY_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    private final FakeSabhaFacts sabhaFacts = new FakeSabhaFacts();
    private final FakeMonths months = new FakeMonths();
    private final MonthlyComplianceQuery query = new MonthlyComplianceQuery(sabhaFacts, months);

    @Test
    void nudgesWhenAMonthlySabhaHasNoOccurrenceAndTheMonthIsPastItsMidpoint() {
        assertThat(query.needsOccurrence(MONTHLY_SABHA, LocalDate.of(2026, 6, 20))).isTrue();
    }

    @Test
    void staysSilentBeforeTheMonthMidpoint() {
        assertThat(query.needsOccurrence(MONTHLY_SABHA, LocalDate.of(2026, 6, 10))).isFalse();
    }

    @Test
    void staysSilentOnceAnOccurrenceExistsThisMonth() {
        months.markPresent(MONTHLY_SABHA, YearMonth.of(2026, 6));
        assertThat(query.needsOccurrence(MONTHLY_SABHA, LocalDate.of(2026, 6, 20))).isFalse();
    }

    @Test
    void neverNudgesAWeeklySabha() {
        assertThat(query.needsOccurrence(WEEKLY_SABHA, LocalDate.of(2026, 6, 20))).isFalse();
    }

    private static final class FakeSabhaFacts implements SabhaFacts {
        private static final SabhaScope ANY_SCOPE = new SabhaScope(null, null, null);
        private static final SabhaSchedule ANY_SLOT =
                new SabhaSchedule(DayOfWeek.SUNDAY, LocalTime.of(9, 0), LocalTime.of(10, 30));

        @Override
        public Optional<SabhaFact> of(UUID sabhaId) {
            if (sabhaId.equals(MONTHLY_SABHA)) {
                return Optional.of(SabhaFact.monthlyAdHoc(sabhaId, ANY_SCOPE, false));
            }
            if (sabhaId.equals(WEEKLY_SABHA)) {
                return Optional.of(SabhaFact.weekly(sabhaId, ANY_SCOPE, ANY_SLOT, false));
            }
            return Optional.empty();
        }

        @Override
        public List<SabhaFact> allWeekly() {
            return List.of();
        }

        @Override
        public Optional<SabhaFact> selectiveIn(UUID kshetraId, String demographic, String track) {
            return Optional.empty();
        }
    }

    private static final class FakeMonths implements OccurrenceCalendar {
        final Set<String> present = new HashSet<>();

        void markPresent(UUID sabhaId, YearMonth month) {
            present.add(sabhaId + "@" + month);
        }

        @Override
        public boolean exists(UUID sabhaId, LocalDate date) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsInMonth(UUID sabhaId, YearMonth month) {
            return present.contains(sabhaId + "@" + month);
        }
    }
}
