package org.sabha.analytics.applicationservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.analytics.domain.Candidate;
import org.sabha.analytics.domain.HomeSabhaHistory;
import org.sabha.analytics.domain.OutcomeKind;
import org.sabha.analytics.domain.Scope;
import org.sabha.analytics.domain.Thresholds;
import org.sabha.analytics.domain.Tier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sabha.analytics.domain.OutcomeKind.ABSENT;
import static org.sabha.analytics.domain.OutcomeKind.CANCELLED;
import static org.sabha.analytics.domain.OutcomeKind.PRESENT;
import static org.sabha.analytics.domain.OutcomeKind.WALK_IN_ELSEWHERE;

class ReEngagementCandidateCalculatorTest {

    private static final UUID RAVI = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID YUVAK_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID SANYUKTA_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    private final FakeHistory history = new FakeHistory();
    private final ReEngagementCandidateCalculator calculator =
            new ReEngagementCandidateCalculator(history, () -> new Thresholds(3, 6));

    @Test
    void threeConsecutiveMissedHomeSabhaOccurrencesMakeAReEngagementCandidate() {
        history.record(RAVI, YUVAK_SABHA, ABSENT, ABSENT, ABSENT);

        List<Candidate> candidates = calculator.candidatesFor(new Scope.OfSabhas(Set.of(YUVAK_SABHA)));

        assertThat(candidates)
                .containsExactly(new Candidate(RAVI, 3, YUVAK_SABHA, Tier.CANDIDATE));
    }

    @Test
    void fewerThanThreeMissedIsNotYetACandidate() {
        history.record(RAVI, YUVAK_SABHA, ABSENT, ABSENT);

        assertThat(calculator.candidatesFor(new Scope.OfSabhas(Set.of(YUVAK_SABHA)))).isEmpty();
    }

    @Test
    void sixConsecutiveMissedEscalatesToPriority() {
        history.record(RAVI, YUVAK_SABHA, ABSENT, ABSENT, ABSENT, ABSENT, ABSENT, ABSENT);

        assertThat(calculator.candidatesFor(new Scope.OfSabhas(Set.of(YUVAK_SABHA))))
                .containsExactly(new Candidate(RAVI, 6, YUVAK_SABHA, Tier.PRIORITY));
    }

    @Test
    void presentAtTheHomeSabhaResetsTheStreak() {
        history.record(RAVI, YUVAK_SABHA, ABSENT, ABSENT, ABSENT, PRESENT);

        assertThat(calculator.candidatesFor(new Scope.OfSabhas(Set.of(YUVAK_SABHA)))).isEmpty();
    }

    @Test
    void aWalkInElsewhereDoesNotResetTheStreak() {
        history.record(RAVI, YUVAK_SABHA, ABSENT, WALK_IN_ELSEWHERE, ABSENT, ABSENT);

        assertThat(calculator.candidatesFor(new Scope.OfSabhas(Set.of(YUVAK_SABHA))))
                .containsExactly(new Candidate(RAVI, 3, YUVAK_SABHA, Tier.CANDIDATE));
    }

    @Test
    void aCancelledOccurrenceDoesNotCountAsMissed() {
        history.record(RAVI, YUVAK_SABHA, ABSENT, CANCELLED, ABSENT);

        assertThat(calculator.candidatesFor(new Scope.OfSabhas(Set.of(YUVAK_SABHA)))).isEmpty();
    }

    @Test
    void streaksAreIndependentPerHomeSabha() {
        history.record(RAVI, YUVAK_SABHA, ABSENT, ABSENT, ABSENT);
        history.record(RAVI, SANYUKTA_SABHA, PRESENT, PRESENT);

        assertThat(calculator.candidatesFor(new Scope.OfSabhas(Set.of(YUVAK_SABHA, SANYUKTA_SABHA))))
                .containsExactly(new Candidate(RAVI, 3, YUVAK_SABHA, Tier.CANDIDATE));
    }

    /** In-memory stand-in for the per-(Person, Home Sabha) chronological outcome stream. */
    private static final class FakeHistory implements HomeSabhaOccurrenceHistory {
        private final List<HomeSabhaHistory> streams = new ArrayList<>();

        void record(UUID personId, UUID homeSabhaId, OutcomeKind... outcomes) {
            streams.add(new HomeSabhaHistory(personId, homeSabhaId, List.of(outcomes)));
        }

        @Override
        public List<HomeSabhaHistory> within(Scope scope) {
            return streams.stream()
                    .filter(s -> scope.includes(s.homeSabhaId()))
                    .toList();
        }
    }
}
