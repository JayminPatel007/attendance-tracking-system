/**
 * A top-level section of the web shell. Mirrors the backend `Section` enum
 * (Slice 9) — the backend decides which sections a user may see and returns the
 * set on `/bff/me`; the shell renders sidebar nav and gates routes from it.
 */
export type Section =
  | 'DASHBOARD'
  | 'ROLE_APPOINTMENT'
  | 'STRUCTURAL_ADMIN'
  | 'SABHA_DEFINITION'
  | 'OCCURRENCE_REOPEN'
  | 'SANCHALAK_PROXY';
