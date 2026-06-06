package org.sabha.analytics.applicationservice;

import java.util.List;
import java.util.UUID;

/**
 * Read port serving the three re-engagement dashboard sections (ADR-0010), each
 * filtered to the slice of the organisation the caller's roles grant. Backed by
 * the candidate projection, not a live calculation.
 */
public interface DashboardQueries {

    /** Section A — KPI strip + candidate headline. */
    DashboardOverview overview(UUID userId);

    /** Section B — the full in-scope candidate list for the filterable table. */
    List<CandidateRow> people(UUID userId);

    /** Section C — Zone → Kshetra → Sabha candidate counts. */
    SabhaTree sabhaTree(UUID userId);
}
