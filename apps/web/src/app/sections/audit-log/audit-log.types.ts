/**
 * Frontend mirror of the analytics context's audit-feed DTOs (Slice 19,
 * ADR-0023). The JSON shapes match `AuditLogBffController` / `AuditEntry`: one
 * common projection over the per-slice provenance tables, newest first. `id` is
 * NOT unique across the feed (a selection nomination row emits both a nominate
 * and a decide entry) — consumers key on `(id, action)`.
 */

/** Closed set the viewer filters on; mirrors the backend `AuditTargetType` enum. */
export type AuditTargetType = 'OCCURRENCE' | 'SABHA' | 'ROLE_ASSIGNMENT' | 'STRUCTURAL' | 'PERSON';

export const AUDIT_TARGET_TYPES: readonly AuditTargetType[] = [
  'OCCURRENCE',
  'SABHA',
  'ROLE_ASSIGNMENT',
  'STRUCTURAL',
  'PERSON',
];

export interface AuditEntry {
  id: string;
  /** ISO instant of the audited act. */
  at: string;
  actorUserId: string | null;
  /** Resolved display name; null means a system act (e.g. auto-materialised transition). */
  actorName: string | null;
  /** Non-null only for a Nirikshak-as-Sanchalak proxy action (Slice 14). */
  onBehalfOfUserId: string | null;
  onBehalfName: string | null;
  targetType: AuditTargetType;
  targetId: string;
  action: string;
  detail: string | null;
}

/**
 * Viewer-side filter; every field optional. `from` / `to` are calendar dates
 * (`yyyy-MM-dd`, `to` inclusive — the server widens it to the next day).
 * `targetType` + `targetId` together are the per-entity drill-down.
 */
export interface AuditFilter {
  targetType?: AuditTargetType;
  targetId?: string;
  actorUserId?: string;
  action?: string;
  from?: string;
  to?: string;
  proxyOnly?: boolean;
}
