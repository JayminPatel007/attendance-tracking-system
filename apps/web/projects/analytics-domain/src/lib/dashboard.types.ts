/**
 * Frontend mirror of the analytics context's re-engagement dashboard DTOs
 * (Slice 15, ADR-0010). The JSON shapes match the backend records consumed via
 * the BFF (`DashboardBffController`): every read is already scoped to the
 * caller's tier server-side, so the web sends no scope parameters.
 */

/** The tier a drifting Person has reached, mirror of the backend `Tier` enum. */
export type Tier = 'CANDIDATE' | 'PRIORITY';

/**
 * One re-engagement candidate as the People table and the overview headline list
 * render it: the drifting Person, the Home Sabha they are drifting from, and
 * their current streak/tier. Matches `CandidateRow`.
 */
export interface CandidateRow {
  personId: string;
  personName: string;
  homeSabhaId: string;
  /** Denormalized `TRACK_DEMOGRAPHIC` token, e.g. `REGULAR_YUVAK`. */
  sabhaKind: string;
  kshetraName: string;
  demographic: string;
  missedStreak: number;
  tier: Tier;
}

/** The KPI strip figures, computed over the caller's full in-scope candidate set. */
export interface Kpis {
  totalCandidates: number;
  priorityCandidates: number;
  sabhasWithCandidates: number;
}

/** Section A payload: the KPI strip plus the candidate headline list. */
export interface DashboardOverview {
  kpis: Kpis;
  headlineCandidates: CandidateRow[];
}

/** A Sabha leaf in the analytics tree (section C). */
export interface SabhaNode {
  sabhaId: string;
  sabhaKind: string;
  candidateCount: number;
}

/** A Kshetra node grouping its Sabhas (section C). */
export interface KshetraNode {
  kshetraId: string;
  kshetraName: string;
  candidateCount: number;
  sabhas: SabhaNode[];
}

/**
 * A Zone node grouping its Kshetras (section C). `zoneId` is `null` for the
 * bucket of Kshetras with no Zone (the tracer seed); the tree renders it as an
 * "Unzoned" group.
 */
export interface ZoneNode {
  zoneId: string | null;
  zoneName: string;
  candidateCount: number;
  kshetras: KshetraNode[];
}

/** Section C payload: Zone → Kshetra → Sabha with counts at every level. */
export interface SabhaTree {
  zones: ZoneNode[];
}

/**
 * The MK-owned re-engagement thresholds: the consecutive-missed counts at which
 * a Person becomes a candidate and then a priority. Invariant
 * `priority >= candidate >= 1` is enforced server-side (422 on violation).
 */
export interface Thresholds {
  candidate: number;
  priority: number;
}

/** One pickable City in the dashboard chip (Slice 17). Mirrors backend `CityOption`. */
export interface CityOption {
  id: string;
  name: string;
}

/**
 * What the dashboard City chip renders (Slice 17, mirror of backend
 * `DashboardAccess.CityChip`). A Sant gets the full City list and their current
 * pick and sees an interactive picker; every other role gets `sant: false` and
 * an empty list, and the shell shows a non-interactive scope indicator instead.
 */
export interface DashboardScopeChip {
  sant: boolean;
  selectedCityId: string | null;
  cities: CityOption[];
}
