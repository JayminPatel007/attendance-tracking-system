package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.SabhaShapeLookup;

import static org.assertj.core.api.Assertions.assertThat;

class MonthlyComplianceQueryTest {

    private static final UUID MONTHLY_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID WEEKLY_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    private final FakeShapes shapes = new FakeShapes();
    private final FakeMonths months = new FakeMonths();
    private final MonthlyComplianceQuery query = new MonthlyComplianceQuery(shapes, months);

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

    private static final class FakeShapes implements SabhaShapeLookup {
        @Override
        public Optional<String> scheduleShapeOf(UUID sabhaId) {
            if (sabhaId.equals(MONTHLY_SABHA)) {
                return Optional.of("MONTHLY_AD_HOC");
            }
            if (sabhaId.equals(WEEKLY_SABHA)) {
                return Optional.of("WEEKLY_RECURRING");
            }
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
