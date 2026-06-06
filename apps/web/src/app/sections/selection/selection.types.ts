/**
 * Frontend mirror of the identity context's selection read-model DTOs (Slice 16,
 * ADR-0006). The JSON shapes match `PendingNominationItem` and `SelectedPersonItem`
 * from `SelectionBffController`: the queue lists the PENDING nominations the
 * Nirdeshak may approve/reject, and the selected list the APPROVED People they may
 * deselect (carrying the person + selective Sabha the deselect action needs).
 */
export interface PendingNomination {
  nominationId: string;
  personId: string;
  personName: string;
  regularSabhaId: string;
  selectiveSabhaId: string;
  /** Demographic enum name, e.g. `YUVAK`. */
  demographic: string;
  /** Selective track, `BSS` or `YSS`. */
  track: string;
  nominatedBy: string;
  nominatedByName: string;
  /** ISO instant the nomination was made. */
  nominatedAt: string;
}

export interface SelectedPerson {
  nominationId: string;
  personId: string;
  personName: string;
  selectiveSabhaId: string;
  demographic: string;
  track: string;
  decidedBy: string;
  decidedByName: string;
  /** ISO instant the nomination was approved. */
  decidedAt: string;
}
