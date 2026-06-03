/**
 * Frontend mirror of the sabha context's structural read/write DTOs (ADR-0009).
 * The JSON shapes match `StructuralCreationController` and `StructuralQueries`.
 */
export type Demographic = 'BAAL' | 'BALIKA' | 'YUVAK' | 'YUVATI' | 'SANYUKTA';
export type Track = 'REGULAR' | 'BSS' | 'YSS';

export const DEMOGRAPHICS: readonly Demographic[] = ['BAAL', 'BALIKA', 'YUVAK', 'YUVATI', 'SANYUKTA'];
export const TRACKS: readonly Track[] = ['REGULAR', 'BSS', 'YSS'];

export interface CityView {
  id: string;
  name: string;
}

export interface ZoneView {
  id: string;
  name: string;
  cityId: string;
  cityName: string;
}

export interface SabhaKindView {
  id: string;
  demographic: Demographic;
  track: Track;
}

export interface KshetraView {
  id: string;
  name: string;
  zoneId: string;
}

/** A Sanyukta kind exists on the Regular track only (CONTEXT, ADR-0009). */
export function isAllowedKind(demographic: Demographic, track: Track): boolean {
  return demographic !== 'SANYUKTA' || track === 'REGULAR';
}
