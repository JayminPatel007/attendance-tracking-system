package org.sabha.analytics.applicationservice;

import java.util.List;

/**
 * Read port serving the three re-engagement dashboard sections (ADR-0010), each
 * filtered to the {@link DashboardScope} the {@link DashboardAccess} engine
 * resolved for the caller: a role-scoped slice for every operational tier and the
 * MK (Slice 15), or a single City for a Sant's universal read (Slice 17). Backed
 * by the candidate projection, not a live calculation.
 */
public interface DashboardQueries {

    /** Section A — KPI strip + candidate headline. */
    DashboardOverview overview(DashboardScope scope);

    /** Section B — the full in-scope candidate list for the filterable table. */
    List<CandidateRow> people(DashboardScope scope);

    /** Section C — Zone → Kshetra → Sabha candidate counts. */
    SabhaTree sabhaTree(DashboardScope scope);
}
