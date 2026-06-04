/**
 * Frontend mirror of the attendance context's Sanchalak-proxy DTOs (Slice 14,
 * ADR-0001). The JSON shapes match `SanchalakProxyBffController`,
 * `ProxySabhaListItem`, and `ProxyOccurrenceItem`: the picker lists the Sabhas the
 * Nirikshak may proxy (each with the informational "last seen" hint), and the
 * toolkit lists the chosen Sabha's Occurrences to cancel / reschedule / re-venue.
 */
export interface ProxySabha {
  sabhaId: string;
  /** Denormalized `TRACK_DEMOGRAPHIC · Kshetra` label, e.g. `REGULAR_YUVAK · Kshetra Tracer`. */
  sabhaLabel: string;
  sanchalakUserId: string | null;
  sanchalakName: string | null;
  /** ISO instant of the latest login / sync / marking, or null if never seen. Informational only. */
  lastSeenAt: string | null;
}

export interface ProxyOccurrence {
  id: string;
  /** ISO date (the effective, rescheduled-or-standing date). */
  effectiveDate: string;
  /** The `OccurrenceState` enum name, e.g. `SCHEDULED`. */
  state: string;
  venue: string;
}

export interface RescheduleRequest {
  date: string;
  startTime: string;
  endTime: string;
}
