package org.sabha.analytics.applicationservice;

import java.util.List;

/**
 * The dashboard overview (section A, ADR-0010): the KPI strip plus the
 * re-engagement candidate headline list, both already scoped to the caller's tier.
 */
public record DashboardOverview(Kpis kpis, List<CandidateRow> headlineCandidates) {

    /** The KPI strip figures, computed over the caller's full in-scope candidate set. */
    public record Kpis(int totalCandidates, int priorityCandidates, int sabhasWithCandidates) {
    }
}
