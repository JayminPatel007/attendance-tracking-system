package org.sabha.analytics.domain;

import java.util.List;
import java.util.UUID;

/**
 * The chronological (oldest-first) outcome stream for one Person against one of
 * their Home Sabhas, merged from that Home Sabha's Occurrences and the Person's
 * Walk-in marks elsewhere. The Calculator derives the current missed streak from
 * the tail of {@code outcomes}.
 */
public record HomeSabhaHistory(UUID personId, UUID homeSabhaId, List<OutcomeKind> outcomes) {

    /**
     * The current run of missed Home Sabha Occurrences (ADR-0010): walk back from
     * the most recent outcome, counting {@code ABSENT}, stopping at the first
     * {@code PRESENT}, and stepping over {@code CANCELLED} / {@code WALK_IN_ELSEWHERE},
     * which neither count nor reset.
     */
    public int currentMissedStreak() {
        int streak = 0;
        for (int i = outcomes.size() - 1; i >= 0; i--) {
            switch (outcomes.get(i)) {
                case ABSENT -> streak++;
                case PRESENT -> {
                    return streak;
                }
                case CANCELLED, WALK_IN_ELSEWHERE -> {
                    // engagement-irrelevant — leave the streak untouched
                }
            }
        }
        return streak;
    }
}
