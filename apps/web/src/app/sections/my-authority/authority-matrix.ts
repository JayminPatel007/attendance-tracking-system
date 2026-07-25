/**
 * The "what can I create or delete here?" model (issue #90) — one static,
 * client-side description of the authority the backend already enforces.
 *
 * Every row below mirrors a shipped rule, so the screen can never promise
 * something the API refuses:
 *
 * <ul>
 *   <li>creation and geographic deletion — `StructuralScopeAuthority`
 *       (ADR-0009, ADR-0024, ADR-0026): the holder of scope at a tier owns the
 *       children one tier below, by current scope rather than who created them;</li>
 *   <li>the Sabha Kind soft-retire — `SabhaKindLifecycleService` (ADR-0026);</li>
 *   <li>appointment and revocation — `AppointmentAuthorization` /
 *       `RoleRevocationService` (ADR-0011, ADR-0025), including the Regional Team
 *       last-one-out guard and the Sah-Nirdeshak cap of two.</li>
 * </ul>
 *
 * It is deliberately data, not a fetch: authority is a property of the tier, and
 * the screen shows every tier so a holder can compare their own against the one
 * above. Keep it in step with those engines when their rules move.
 */

/** The tiers that hold create/delete authority, plus the tier that pointedly doesn't. */
export type ActorTier = 'mk' | 'regional-team' | 'sanyojak' | 'nirdeshak' | 'sah-nirdeshak';

/** Top-down, so the screen reads as a chain of delegation. */
export const ACTOR_TIERS: readonly ActorTier[] = [
  'mk',
  'regional-team',
  'sanyojak',
  'nirdeshak',
  'sah-nirdeshak',
];

/** What "delete" means for a given kind of thing (ADR-0026). */
export type DeleteKind = 'block-if-non-empty' | 'soft-retire' | 'revoke';

/** One thing a tier may bring into being, and the rule that governs removing it. */
export interface AuthorityItem {
  name: string;
  /**
   * How this tier brings the item about, in the domain's own words — a Sant is
   * *provisioned* as an administrative act, never "appointed" (CONTEXT.md).
   */
  verb: 'Create' | 'Appoint' | 'Provision';
  /** The scope the creation/appointment is bound to. */
  scope: string;
  delete: DeleteKind;
  /** The concrete guard on that removal, as the backend enforces it. */
  deleteNote: string;
}

/** Everything one tier may do, as the screen presents it. */
export interface ActorAuthority {
  tier: ActorTier;
  label: string;
  /** The scope the tier is held at, e.g. "per (Kshetra, demographic)". */
  scopeLabel: string;
  structures: AuthorityItem[];
  roles: AuthorityItem[];
  /** Powers that are neither create nor delete — populated only where they are the whole story. */
  operational: string[];
  /** Shown beneath the operational powers, spelling out the absence of create/delete authority. */
  noAuthorityNote: string | null;
}

export const DELETE_LEGEND: readonly { kind: DeleteKind; label: string; meaning: string }[] = [
  {
    kind: 'block-if-non-empty',
    label: 'Block-if-non-empty',
    meaning:
      'Geography and Sabhas. Allowed only while nothing lives underneath — you dismantle top-down, and recorded attendance is never destroyed.',
  },
  {
    kind: 'soft-retire',
    label: 'Soft-retire',
    meaning:
      'The Sabha Kind. Marked inactive so nothing new of that kind can be created while existing Sabhas drain; never hard-deleted, and reversible.',
  },
  {
    kind: 'revoke',
    label: 'Revoke assignment',
    meaning:
      'Role-holders. The assignment is marked revoked, not erased: the Person stays, and their structures and appointees pass to the next holder of the scope. Losing the last role withdraws login.',
  },
];

const AUTHORITY: Record<ActorTier, ActorAuthority> = {
  mk: {
    tier: 'mk',
    label: 'Madhyastha Karyalaya',
    scopeLabel: 'State — one body, all demographics and tracks',
    structures: [
      {
        name: 'City',
        verb: 'Create',
        scope: 'anywhere in the State',
        delete: 'block-if-non-empty',
        deleteNote: 'Blocked while the City still has Zones.',
      },
      {
        name: 'Sabha Kind',
        verb: 'Create',
        scope: 'a (demographic, track) pair, registered once',
        delete: 'soft-retire',
        deleteNote:
          'Retire stops new Sabhas, roles and Home Sabhas of the kind; existing ones drain. You can reactivate it.',
      },
    ],
    roles: [
      {
        name: 'Regional Team member',
        verb: 'Appoint',
        scope: 'per (City, demographic) — you seed the first, after which the team grows itself',
        delete: 'revoke',
        deleteNote: 'Refused on the last remaining member of a (City, demographic).',
      },
      {
        name: 'Sant',
        verb: 'Provision',
        scope:
          'per (City, demographic) — an administrative act creating their login; the position itself exists outside the system',
        delete: 'revoke',
        deleteNote: 'Revoking their only role withdraws the login; the Person record stays.',
      },
    ],
    operational: [],
    noAuthorityNote: null,
  },

  'regional-team': {
    tier: 'regional-team',
    label: 'Regional Team',
    scopeLabel: 'City — per (City, demographic), track-shared',
    structures: [
      {
        name: 'Zone',
        verb: 'Create',
        scope: 'within a City you are a member of (ADR-0024)',
        delete: 'block-if-non-empty',
        deleteNote: 'Blocked while the Zone still has Kshetras.',
      },
    ],
    roles: [
      {
        name: 'Regional Team member (peer)',
        verb: 'Appoint',
        scope: 'the same (City, demographic) — the tier grows itself (ADR-0025)',
        delete: 'revoke',
        deleteNote:
          'Refused on the last remaining member of a (City, demographic), so the tier can never be emptied.',
      },
      {
        name: 'Sanyojak',
        verb: 'Appoint',
        scope: 'per (Zone, demographic) within your City',
        delete: 'revoke',
        deleteNote:
          "Their Kshetras and the Nirdeshaks they appointed stay put — the next Sanyojak of the Zone inherits them.",
      },
    ],
    operational: [],
    noAuthorityNote: null,
  },

  sanyojak: {
    tier: 'sanyojak',
    label: 'Sanyojak',
    scopeLabel: 'Zone — per (Zone, demographic), track-shared',
    structures: [
      {
        name: 'Kshetra',
        verb: 'Create',
        scope: 'within your own Zone',
        delete: 'block-if-non-empty',
        deleteNote: 'Blocked while the Kshetra still has Sabhas.',
      },
    ],
    roles: [
      {
        name: 'Nirdeshak',
        verb: 'Appoint',
        scope: 'per (Kshetra, demographic) in your Zone',
        delete: 'revoke',
        deleteNote:
          'You may revoke a Nirdeshak you did not appoint — authority is by your current scope, not by who appointed them.',
      },
    ],
    operational: [],
    noAuthorityNote: null,
  },

  nirdeshak: {
    tier: 'nirdeshak',
    label: 'Nirdeshak',
    scopeLabel: 'Kshetra — per (Kshetra, demographic), Regular and selective tracks',
    structures: [
      {
        name: 'Sabha',
        verb: 'Create',
        scope: 'within your (Kshetra, demographic); creating one appoints its Sanchalak',
        delete: 'block-if-non-empty',
        deleteNote: 'Blocked once the Sabha has any recorded Occurrence.',
      },
    ],
    roles: [
      {
        name: 'Sanchalak',
        verb: 'Appoint',
        scope: 'one per Sabha in your scope',
        delete: 'revoke',
        deleteNote: 'The Sabha and its history stay; the next Sanchalak inherits them.',
      },
      {
        name: 'Sah-Sanchalak',
        verb: 'Appoint',
        scope: 'deputy on a Sabha in your scope',
        delete: 'revoke',
        deleteNote: 'Revoking their only role withdraws the login; the Person record stays.',
      },
      {
        name: 'Nirikshak',
        verb: 'Appoint',
        scope: 'per (Kshetra, demographic), overseeing 3–4 of your Sabhas',
        delete: 'revoke',
        deleteNote: 'Revoking their only role withdraws the login; the Person record stays.',
      },
      {
        name: 'Sah-Nirdeshak',
        verb: 'Appoint',
        scope: 'at most 2 per (Kshetra, demographic) (ADR-0025)',
        delete: 'revoke',
        deleteNote: 'Revoking one frees a slot against the cap of 2.',
      },
    ],
    operational: [],
    noAuthorityNote: null,
  },

  'sah-nirdeshak': {
    tier: 'sah-nirdeshak',
    label: 'Sah-Nirdeshak',
    scopeLabel: 'Kshetra — deputy for the same (Kshetra, demographic) as the Nirdeshak',
    structures: [],
    roles: [],
    operational: [
      'Reopen a Finalized Occurrence on a Sabha in your (Kshetra, demographic), with a reason.',
      'See the same analytics as the Nirdeshak, over the same Kshetra scope.',
    ],
    noAuthorityNote:
      'The Sah-Nirdeshak is an operational backstop, not an administrator: no structural creation, no appointments, and no deletions or revocations — for now (ADR-0025 §3). The Sanchalak-proxy toolkit is the Nirikshak’s, on the Sabhas assigned to them; it is not held here.',
  },
};

export function authorityFor(tier: ActorTier): ActorAuthority {
  return AUTHORITY[tier];
}
