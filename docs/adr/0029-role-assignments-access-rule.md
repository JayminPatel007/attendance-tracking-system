# `role_assignments` is identity-owned: read-models may join it, authority checks go through ports

**Status**: accepted (issue #79). Records a rule the codebase already mostly follows and consolidates the last clear offenders. Feeds the #68 design spike ([ADR-0027](0027-no-shared-granted-scope-module-behind-the-authorization-engines.md)).

The `role_assignments` table is owned by the **identity** bounded context: identity writes every row (appointment, MK/Sant bootstrap, proxy assignment) and is the authority on what a row *means*. But the table is also the single richest source of "who is allowed to do / see what", so every other context wants to read it. Left ungoverned, that produces hand-written `role = '…'` SQL scattered across all four services — which is exactly what the 2026-06-10 code-quality review found: the `role = 'SANT'` existence check alone was copy-pasted into three adapters in two contexts (analytics `JdbcSantDirectory`, identity `JdbcSantMembership`, identity `JdbcReissueAuthorityLookup.isSant`), each repeating the caveat that SANT lives outside the operational `Role` enum.

Two different things read this table, and they are not the same kind of access:

## The rule

1. **CQRS read-models may join across context tables, including `role_assignments`.**
   A read adapter assembling a projection (the audit feed, a dashboard roster, the occurrence-reopen view) is allowed to `JOIN role_assignments` directly from its own data-access module. This is the established read-side latitude of [ADR-0008](0008-single-bounded-context-with-internal-seams.md) (one database, internal seams) and [ADR-0023](0023-audit-log-read-model-and-viewer-authority.md) (the audit log *is* a read-model over existing tables). Examples that stay as-is: `JdbcAuditFeed`, `JdbcAuditScopeLookup`, `JdbcDashboardQueries`, `JdbcCurrentRosterQuery`, `JdbcOccurrenceReopenQueries`.

2. **Authority and membership checks go through common-domain ports owned by identity.**
   A *decision* — "is this caller a Sant / an MK member / the appointer / a holder of tier X at scope Y?" — is not a projection; it is a fact about identity's domain that another context is consulting. It must cross the seam through a **common-domain port implemented in identity-data-access**, per [ADR-0019](0019-bounded-context-module-taxonomy.md), not through foreign hand-written SQL. The existing well-formed examples: `RoleAssignmentLookup`, `MadhyasthaKaryalayaLookup`, `SanyojakZoneLookup`, `StructuralHierarchyLookup` — and now `SantLookup`.

The line between the two: a read-model returns *data to render*; a lookup returns *a yes/no (or scope) the caller branches on*. When in doubt, if removing the call would change an authorization outcome, it is a check and belongs behind a port.

## What this issue moved

`SantLookup` is promoted to common-domain with one `JdbcSantLookup` adapter in identity-data-access. The three duplicate Sant adapters and the `isSant` method on `ReissueAuthorityLookup` are deleted; `DashboardAccess`, `AuditLogAccess`, `WebSessionService`, and `PasswordReissueService` consume the shared port. The `'SANT'` string and the "outside the operational `Role` enum" caveat now live once, in the `OversightRole` enum (alongside `MADHYASTHA_KARYALAYA`), which the MK adapter also adopts.

## Scope and non-goals

This ADR moves the *clear* offenders only — the Sant membership checks consolidated by #79. The larger question of whether the four authorization engines should share a granted-scope module was the #68 spike, and was **rejected** in [ADR-0027](0027-no-shared-granted-scope-module-behind-the-authorization-engines.md): the engines ask structurally different questions and their per-engine ports already pull their weight. This rule and that decision are consistent — both say "small, shape-matched, identity-owned ports", not "one general lookup". Future cross-context reads of `role_assignments` should be classified by the rule above before any SQL is written in a foreign module.
