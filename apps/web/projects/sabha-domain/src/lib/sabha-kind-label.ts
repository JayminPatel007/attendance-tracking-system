const DEMOGRAPHIC_LABELS: Record<string, string> = {
  BAAL: 'Baal',
  BALIKA: 'Balika',
  YUVAK: 'Yuvak',
  YUVATI: 'Yuvati',
  SANYUKTA: 'Sanyukta',
};

/** Human label for a bare demographic token, e.g. `YUVAK` → "Yuvak". */
export function demographicLabel(demographic: string): string {
  return DEMOGRAPHIC_LABELS[demographic] ?? demographic;
}

/**
 * Human label for a denormalized `TRACK_DEMOGRAPHIC` kind token, e.g.
 * `REGULAR_YUVAK` → "Yuvak (Regular)". This is the canonical decoder for the
 * kind tokens the backend denormalizes onto read models (Occurrences, the
 * re-engagement dashboard); features should reuse it rather than re-deriving the
 * split.
 */
export function kindLabel(sabhaKind: string): string {
  const underscore = sabhaKind.indexOf('_');
  if (underscore < 0) {
    return sabhaKind;
  }
  const track = sabhaKind.slice(0, underscore);
  const demographic = sabhaKind.slice(underscore + 1);
  const titleTrack = track.charAt(0) + track.slice(1).toLowerCase();
  return `${demographicLabel(demographic)} (${titleTrack})`;
}
