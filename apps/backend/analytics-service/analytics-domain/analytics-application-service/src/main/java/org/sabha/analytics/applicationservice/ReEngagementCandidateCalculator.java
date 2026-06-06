package org.sabha.analytics.applicationservice;

import java.util.List;
import java.util.Optional;

import org.sabha.analytics.domain.Candidate;
import org.sabha.analytics.domain.HomeSabhaHistory;
import org.sabha.analytics.domain.Scope;
import org.sabha.analytics.domain.Thresholds;
import org.springframework.stereotype.Service;

/**
 * The Re-engagement Candidate Calculator (ADR-0010): the deep module behind the
 * dashboard's headline list. Given a Scope it returns the Persons drifting from a
 * Home Sabha. The streak rule itself lives on {@link HomeSabhaHistory}; this
 * service applies the MK-owned candidate/priority thresholds on top of it.
 */
@Service
public class ReEngagementCandidateCalculator {

    private final HomeSabhaOccurrenceHistory history;
    private final ThresholdConfig config;

    public ReEngagementCandidateCalculator(HomeSabhaOccurrenceHistory history, ThresholdConfig config) {
        this.history = history;
        this.config = config;
    }

    public List<Candidate> candidatesFor(Scope scope) {
        Thresholds thresholds = config.current();
        return history.within(scope).stream()
                .flatMap(stream -> toCandidate(stream, thresholds).stream())
                .toList();
    }

    private Optional<Candidate> toCandidate(HomeSabhaHistory stream, Thresholds thresholds) {
        int streak = stream.currentMissedStreak();
        return thresholds.classify(streak)
                .map(tier -> new Candidate(stream.personId(), streak, stream.homeSabhaId(), tier));
    }
}
