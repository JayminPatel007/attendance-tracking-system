/**
 * Frontend mirror of the identity context's appointment DTOs (ADR-0011). The
 * JSON shapes match `RoleAppointmentController` — `AppointmentRequest`,
 * `AppointmentResponse`, the directory `PersonResponse`, and `NameCandidate`.
 */
export type Demographic = 'BAAL' | 'BALIKA' | 'YUVAK' | 'YUVATI' | 'SANYUKTA';
export type Gender = 'MALE' | 'FEMALE';

export const DEMOGRAPHICS: readonly Demographic[] = ['BAAL', 'BALIKA', 'YUVAK', 'YUVATI', 'SANYUKTA'];

/**
 * The roles the web flow can fill, paired with the scope shape each needs. A
 * Sabha-scoped role names a Sabha; the Kshetra / Zone / City tiers name that id
 * plus a demographic. Mirrors the backend `AppointableRole`.
 */
export type AppointableRole =
  | 'SANCHALAK'
  | 'SAH_SANCHALAK'
  | 'NIRIKSHAK'
  | 'SAH_NIRDESHAK'
  | 'NIRDESHAK'
  | 'SANYOJAK'
  | 'REGIONAL_TEAM'
  | 'SANT';

export type ScopeKind = 'SABHA' | 'KSHETRA' | 'ZONE' | 'CITY';

export const ROLE_SCOPE: Record<AppointableRole, ScopeKind> = {
  SANCHALAK: 'SABHA',
  SAH_SANCHALAK: 'SABHA',
  NIRIKSHAK: 'KSHETRA',
  SAH_NIRDESHAK: 'KSHETRA',
  NIRDESHAK: 'KSHETRA',
  SANYOJAK: 'ZONE',
  REGIONAL_TEAM: 'CITY',
  SANT: 'CITY',
};

export interface NewPersonPayload {
  fullName: string;
  gender: Gender;
  dateOfBirth?: string | null;
  mobile?: string | null;
  guardianPersonId?: string | null;
  homeSabhaId: string;
  overrideDuplicateWarning: boolean;
}

export interface AppointmentRequest {
  role: AppointableRole;
  sabhaId?: string | null;
  kshetraId?: string | null;
  zoneId?: string | null;
  cityId?: string | null;
  demographic?: Demographic | null;
  existingPersonId?: string | null;
  newPerson?: NewPersonPayload | null;
  username: string;
  rawPassword: string;
}

export interface NameCandidate {
  personId: string;
  fullName: string;
  homeSabhas: string[];
}

export interface PersonResponse {
  id: string;
  fullName: string;
  gender: Gender;
  dateOfBirth: string | null;
  mobile: string | null;
  guardianPersonId: string | null;
}

export interface AppointmentResponse {
  personId: string | null;
  userId: string | null;
  assignmentId: string | null;
  candidates: NameCandidate[];
  requiresOverride: boolean;
}
