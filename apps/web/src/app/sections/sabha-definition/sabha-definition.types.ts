/**
 * Frontend mirror of the identity context's Sabha-definition DTOs (ADR-0012,
 * ADR-0011). The JSON shapes match `SabhaDefinitionController` — one request
 * defines the standing Sabha and appoints its Sanchalak (and optional
 * Sah-Sanchalak) in a single transaction.
 */
import { NameCandidate } from 'identity-domain';
import { Demographic, Track } from 'sabha-domain';

/** Java `DayOfWeek` is serialized by its enum name. */
export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export const DAYS_OF_WEEK: readonly DayOfWeek[] = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
];

/**
 * An appointee named in the definition: an existing Person picked from the
 * Directory, plus the auto-suggested initial credentials. Mirrors the backend
 * `AppointeePayload` (the inline new-Person path is omitted here — the standing
 * Sabha has no Occurrence to home a brand-new Person against yet).
 */
export interface AppointeePayload {
  existingPersonId: string;
  username: string;
  rawPassword: string;
}

export interface DefineSabhaRequest {
  kshetraId: string;
  sabhaKindId: string;
  weekly: boolean;
  dayOfWeek?: DayOfWeek | null;
  startTime?: string | null;
  endTime?: string | null;
  standingVenue: string;
  sanchalak: AppointeePayload;
  sahSanchalak?: AppointeePayload | null;
}

/**
 * A Sabha in the Nirdeshak's management list (ADR-0026), mirroring the backend
 * `StructuralQueries.SabhaView`. {@code occurrenceCount} gates deletion: the
 * delete button is disabled while it is non-zero (block-if-non-empty).
 */
export interface SabhaSummary {
  id: string;
  kshetraId: string;
  kshetraName: string;
  demographic: Demographic;
  track: Track;
  standingVenue: string;
  occurrenceCount: number;
}

export interface DefineSabhaResponse {
  sabhaId: string | null;
  sanchalakAssignmentId: string | null;
  sahSanchalakAssignmentId: string | null;
  candidates: NameCandidate[];
  requiresOverride: boolean;
}
