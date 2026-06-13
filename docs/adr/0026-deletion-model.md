# Deletion model: block-if-non-empty for geography, soft-retire for Sabha Kind, revoke-with-inheritance for roles

**Status**: accepted. Companion to [ADR-0024](0024-zone-creation-moves-to-regional-team.md) and [ADR-0025](0025-scope-based-appointment-rt-self-replication-sah-nirdeshak.md), which introduced deletion authority at every tier. This ADR defines what "delete" *means*. The overriding constraint: the system is audit-heavy ([ADR-0023](0023-audit-log-read-model-and-viewer-authority.md)) and attendance history must never be destroyed.

"Delete" is not one operation. The target's kind determines the semantics:

## Geographic entities (Zone, Kshetra, Sabha) — block-if-non-empty

Deletion is allowed only when the entity has no live children and no recorded Occurrences/attendance beneath it. To remove a Zone you must first dismantle its Kshetras → Sabhas. The UI surfaces the reason a delete is blocked ("has 6 Kshetras"). Rejected: **cascade hard-delete** (irreversibly erases attendance history, contradicting ADR-0023) and **soft-delete/deactivate** (leaves "deleted" geography lingering with no lifecycle reason to keep it).

## Sabha Kind — soft-retire

A Sabha Kind is reference data the whole type system hangs off (every node can host it, roles scope by it, People have a Home Sabha per kind). It is **retired**, not deleted: marked inactive so no new Sabhas, roles, or Home Sabhas of that kind can be created, while existing ones drain naturally. Block-if-non-empty is wrong here because winding a program down is a gradual lifecycle, not a mistaken-creation undo — you need to stop new growth *while* existing Sabhas run out.

## Role-holders — revoke with inheritance

Deleting a role-holder means **revoking that one role assignment**. The Person/User record persists (the User loses login only when their *last* role is revoked); the structures they created and the roles they appointed stay attached to the *scope* and are inherited by the next holder — no cascade (per [ADR-0025](0025-scope-based-appointment-rt-self-replication-sah-nirdeshak.md)). The one extra guard is the Regional Team **last-one-out** rule.

## Consequences

- `sabha_kinds` needs an active/retired marker (e.g. `retired_at`); creation paths for Sabhas, roles, and Home Sabhas must reject retired kinds.
- Delete endpoints for Zone/Kshetra/Sabha enforce emptiness and return a human-readable blocking reason.
- Role revocation is a state change carrying `revokedBy`/`revokedAt`, not a row deletion — the audit trail and `appointedBy` references survive.
