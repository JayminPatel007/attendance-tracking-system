package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.CallerResolver;
import org.sabha.common.SabhaShapeLookup;

import static org.assertj.core.api.Assertions.assertThat;

class ListSanchalakMonthlySabhasUseCaseTest {

    private static final UUID SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000000f0");
    private static final UUID SANCHALAK = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final UUID SABHA_DUE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID SABHA_COVERED = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    private final FakeCallers callers = new FakeCallers();
    private final FakeSanchalakSabhas sabhas = new FakeSanchalakSabhas();
    private final FakeShapes shapes = new FakeShapes();
    private final FakeMonths months = new FakeMonths();
    private final MonthlyComplianceQuery compliance = new MonthlyComplianceQuery(shapes, months);
    // Past the midpoint of June 2026, so a Sabha with no Occurrence this month is "due".
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-20T08:00:00Z"), ZoneOffset.UTC);

    private final ListSanchalakMonthlySabhasUseCase useCase =
            new ListSanchalakMonthlySabhasUseCase(callers, sabhas, compliance, clock);

    @Test
    void listsTheCallersMonthlySabhasEachWithItsComplianceNudgeFlag() {
        callers.map(SUBJECT, SANCHALAK);
        sabhas.add(SANCHALAK, new SanchalakSabhasQuery.MonthlyAdHocSabha(SABHA_DUE, "YSS_YUVAK", "Hall A"));
        sabhas.add(SANCHALAK, new SanchalakSabhasQuery.MonthlyAdHocSabha(SABHA_COVERED, "BSS_BAAL", "Hall B"));
        months.markPresent(SABHA_COVERED, YearMonth.of(2026, 6));

        List<MonthlySabha> result = useCase.execute(SUBJECT);

        assertThat(result).extracting(MonthlySabha::sabhaId).containsExactly(SABHA_DUE, SABHA_COVERED);
        assertThat(result).extracting(MonthlySabha::standingVenue).containsExactly("Hall A", "Hall B");
        assertThat(result.get(0).needsOccurrence()).isTrue();
        assertThat(result.get(1).needsOccurrence()).isFalse();
    }

    @Test
    void returnsEmptyWhenTheCallerHasNoLocalUser() {
        assertThat(useCase.execute(SUBJECT)).isEmpty();
    }

    private static final class FakeCallers implements CallerResolver {
        private final java.util.Map<UUID, UUID> map = new java.util.HashMap<>();

        void map(UUID subject, UUID userId) {
            map.put(subject, userId);
        }

        @Override
        public Optional<UUID> resolveUserId(UUID keycloakSubject) {
            return Optional.ofNullable(map.get(keycloakSubject));
        }
    }

    private static final class FakeSanchalakSabhas implements SanchalakSabhasQuery {
        private final java.util.Map<UUID, List<MonthlyAdHocSabha>> byUser = new java.util.HashMap<>();

        void add(UUID userId, MonthlyAdHocSabha sabha) {
            byUser.computeIfAbsent(userId, k -> new java.util.ArrayList<>()).add(sabha);
        }

        @Override
        public List<MonthlyAdHocSabha> monthlyAdHocFor(UUID sanchalakUserId) {
            return byUser.getOrDefault(sanchalakUserId, List.of());
        }
    }

    private static final class FakeShapes implements SabhaShapeLookup {
        @Override
        public Optional<String> scheduleShapeOf(UUID sabhaId) {
            return Optional.of("MONTHLY_AD_HOC");
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
