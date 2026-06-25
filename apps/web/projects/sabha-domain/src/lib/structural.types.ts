/**
 * Frontend mirror of the sabha context's structural read/write DTOs (ADR-0009).
 * The JSON shapes match `StructuralCreationController` and `StructuralQueries`.
 * Shared by the structural-admin and Sabha-definition screens.
 */
export type Demographic = 'BAAL' | 'BALIKA' | 'YUVAK' | 'YUVATI' | 'SANYUKTA';
export type Track = 'REGULAR' | 'BSS' | 'YSS';

export const DEMOGRAPHICS: readonly Demographic[] = ['BAAL', 'BALIKA', 'YUVAK', 'YUVATI', 'SANYUKTA'];
export const TRACKS: readonly Track[] = ['REGULAR', 'BSS', 'YSS'];

export interface CityView {
  id: string;
  name: string;
  /** Live child Zones; delete is blocked while non-zero (ADR-0026). */
  zoneCount: number;
}

export interface ZoneView {
  id: string;
  name: string;
  cityId: string;
  cityName: string;
  /** Live child Kshetras; delete is blocked while non-zero (ADR-0026). */
  kshetraCount: number;
}

export interface SabhaKindView {
  id: string;
  demographic: Demographic;
  track: Track;
  /** ISO instant once the kind has been soft-retired (ADR-0026); null/absent while active. */
  retiredAt?: string | null;
}

export interface KshetraView {
  id: string;
  name: string;
  zoneId: string;
  /** Live child Sabhas; delete is blocked while non-zero (ADR-0026). */
  sabhaCount: number;
}

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
