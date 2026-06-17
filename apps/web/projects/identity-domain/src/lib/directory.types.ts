/**
 * Frontend mirror of the identity context's Directory DTOs (ADR-0011, ADR-0013).
 * The JSON shapes match the `/bff/directory/search` responses: a single
 * `PersonResponse` for a mobile hit, a list of `NameCandidate` for a
 * name-within-Kshetra search. Shared by every flow that picks a Person from the
 * Directory (role appointment, Sabha definition).
 */
export type Gender = 'MALE' | 'FEMALE';

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
