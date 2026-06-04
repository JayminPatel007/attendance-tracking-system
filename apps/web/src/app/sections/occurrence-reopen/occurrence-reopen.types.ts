/**
 * Frontend mirror of the attendance context's reopen DTOs (Slice 13, ADR-0001).
 * The JSON shape matches `OccurrenceReopenBffController` / `ReopenListItem`: the
 * two-pane screen lists the Occurrences the caller may reopen, and `reopened` /
 * `lastReopenReason` are derived from the state-transition audit log (the badge
 * has no denormalized column).
 */
export interface OccurrenceListItem {
  occurrenceId: string;
  /** ISO date (the effective, rescheduled-or-standing date). */
  date: string;
  /** The `OccurrenceState` enum name, e.g. `FINALIZED`. */
  state: string;
  kshetraName: string;
  /** Denormalized `TRACK_DEMOGRAPHIC` token, e.g. `REGULAR_YUVAK`. */
  sabhaKind: string;
  venue: string;
  reopened: boolean;
  lastReopenReason: string | null;
}

export interface ReopenRequest {
  reason: string;
}
