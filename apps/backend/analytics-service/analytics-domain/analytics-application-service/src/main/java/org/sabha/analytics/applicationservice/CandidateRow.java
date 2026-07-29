package org.sabha.analytics.applicationservice;

import java.util.UUID;

import org.sabha.analytics.domain.Tier;

/**
 * One re-engagement candidate as the dashboard's People analytics table and the
 * overview headline list render it (ADR-0010): the drifting Person, the Home Sabha
 * they are drifting from, and their current streak/tier.
 */
public record CandidateRow(
        UUID personId,
        String personName,
        UUID homeSabhaId,
        String sabhaKind,
        String kshetraName,
        String demographic,
        int missedStreak,
        Tier tier) {
}
