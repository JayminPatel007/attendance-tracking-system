# Appointment is scope-based; the Regional Team is self-replicating; Sah-Nirdeshak holds no appointment authority

**Status**: accepted. **Refines and partly supersedes [ADR-0011](0011-role-appointment-authority.md)**. The appointment *chain* in ADR-0011 still holds (who appoints whom); this ADR fixes three things ADR-0011 left implicit or stated differently: how authority is bound, the Regional Team's ability to grow itself, and what a Sah-Nirdeshak may actually do.

The scope lattice is unchanged — per-demographic, track-shared above Sanchalak, Nirikshak Regular-track only. Only *who may exercise* appointment/revocation authority changes.

## 1. Authority is by scope, not by creator

A role-holder may appoint **and revoke** any assignment within the scope they hold, regardless of who originally made it. A replacement Nirdeshak can revoke a Sanchalak appointed by their predecessor. This follows ADR-0011's inheritance model: appointees and the structures they own attach to the *scope*, not to the person who created them, so removing an upper-tier holder leaves everything beneath it intact for the successor to inherit (no cascade). The rejected alternative — binding delete/revoke to the original appointer — was discarded because it strands appointees whenever an appointer leaves.

## 2. The Regional Team is self-replicating

Any Regional Team member may appoint **and revoke peer** Regional Team members within the same (City, demographic). The system's only guard is **last-one-out**: it refuses to revoke the final remaining member of a (City, demographic), so the tier can never be emptied. The first member of a (City, demographic) is still created by the Madhyastha Karyalaya (ADR-0011).

Considered and rejected: (a) MK-only appointment of Regional Team members — too far from the City-level pulse and a constant round-trip; (b) creator-bound deletion (a member can only revoke peers they created) — inconsistent with the scope-based rule above. The accepted model permits intra-team politics (member A revokes member B); that risk is accepted as the price of self-serve growth, bounded only by the last-one-out guard.

## 3. Sah-Nirdeshak: operational backstop, not an administrator

The Sah-Nirdeshak is still appointed by the Nirdeshak (ADR-0011), now **capped at two per (Kshetra, demographic)**. ADR-0011 framed the role as a "co-Nirdeshak"; this ADR amends that. A Sah-Nirdeshak **holds no appointment, structural-creation, or deletion authority** ("for now"). They retain the **operational/proxy** half of the backstop — reopening a Finalized Occurrence, acting on the Kshetra's Sabhas during the Nirdeshak's absence — and the **same analytics view** as the Nirdeshak. The backstop is operational, not administrative.

## Consequences

- Every role-assignment record carries `appointedBy`/`appointedAt` (ADR-0011) and now also `revokedBy`/`revokedAt` for the audit trail; revocation is a state change, not a row deletion (see [ADR-0026](0026-deletion-model.md)).
- Authority checks resolve against the actor's *current* scope assignments, never against `createdBy`/`appointedBy`.
- The Sah-Nirdeshak cap (2) is enforced at appointment time per (Kshetra, demographic).
