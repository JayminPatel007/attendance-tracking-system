package org.sabha.analytics.domain;

import java.util.Optional;

/**
 * The MK-owned re-engagement thresholds (ADR-0010): the consecutive-missed counts
 * at which a Person becomes a {@link Tier#CANDIDATE} and then a {@link Tier#PRIORITY}.
 * Owns both the invariant ({@code priority >= candidate >= 1}) and the tier policy,
 * so the Calculator never re-derives the comparison.
 */
public record Thresholds(int candidate, int priority) {

    public Thresholds {
        if (candidate < 1) {
            throw new InvalidThresholdsException("candidate threshold must be at least 1, was " + candidate);
        }
        if (priority < candidate) {
            throw new InvalidThresholdsException(
                    "priority threshold (" + priority + ") must be at least the candidate threshold (" + candidate + ")");
        }
    }

    /** The tier a given missed streak earns, or empty if it is not yet a candidate. */
    public Optional<Tier> classify(int missedStreak) {
        if (missedStreak >= priority) {
            return Optional.of(Tier.PRIORITY);
        }
        if (missedStreak >= candidate) {
            return Optional.of(Tier.CANDIDATE);
        }
        return Optional.empty();
    }
}
