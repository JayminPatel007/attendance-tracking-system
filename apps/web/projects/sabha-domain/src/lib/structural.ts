import { SabhaKindView } from 'shared-data-access';

/**
 * The Sabha-kind vocabulary and the two structural rules the admin screens share
 * (ADR-0009, ADR-0026).
 *
 * The demographic and track vocabularies are the contract's own — aliased off
 * the generated {@link SabhaKindView}, which is where the pair is defined,
 * rather than restated. The read/write view shapes that used to sit beside them
 * are the generated models themselves now (issue #131); what remains here is the
 * behaviour no wire shape carries.
 */
export type Demographic = SabhaKindView.DemographicEnum;
export type Track = SabhaKindView.TrackEnum;

export const DEMOGRAPHICS: readonly Demographic[] = Object.values(SabhaKindView.DemographicEnum);
export const TRACKS: readonly Track[] = Object.values(SabhaKindView.TrackEnum);

/** A Sanyukta kind exists on the Regular track only (CONTEXT, ADR-0009). */
export function isAllowedKind(demographic: Demographic, track: Track): boolean {
  return demographic !== 'SANYUKTA' || track === 'REGULAR';
}

/**
 * The block-if-non-empty reason shown beside a disabled delete button (ADR-0026),
 * or `null` when the entity is empty and deletable. The wording is kept
 * byte-identical to the backend `StructuralNotEmptyException` ("has 1 Zone" /
 * "has 6 Kshetras") so a proactive disable and a server 409 read the same.
 */
export function notEmptyReason(count: number, noun: string): string | null {
  if (count <= 0) {
    return null;
  }
  return `has ${count} ${noun}${count === 1 ? '' : 's'}`;
}
