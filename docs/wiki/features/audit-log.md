---
type: feature
title: Audit Log
description: Reading who did what, assembled at query time from the rows the acts were recorded on.
aliases: [audit, audit trail, audit viewer, who did what, provenance, on behalf of, proxy filter, oversight read]
tags: [audit, bff]
source_paths: [
  apps/backend/analytics-service/*/src/main/**,
  apps/backend/identity-service/identity-domain/identity-domain-core/src/main/**,
  apps/web/src/app/sections/audit-log/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-19/**,
  docs/adr/0022-*.md,
  docs/adr/0023-*.md,
  docs/adr/0029-*.md,
  CONTEXT.md
]
issues: [20, 80, 131]
sources:
  - { id: adr-0022, title: "Web session via a Backend-for-Frontend with an HTTP-only cookie", resource: ../../adr/0022-web-session-via-bff-http-only-cookie.md }
  - { id: adr-0023, title: "Audit log is a read-model over existing tables, viewable by Nirdeshak and above within scope", resource: ../../adr/0023-audit-log-read-model-and-viewer-authority.md }
  - { id: adr-0029, title: "`role_assignments` is identity-owned: read-models may join it, authority checks go through ports", resource: ../../adr/0029-role-assignments-access-rule.md }
  - { id: context, title: "CONTEXT.md — Nirdeshak, Sanyojak, Regional Team, Sant, Madhyastha Karyalaya, Nirikshak", resource: ../../../CONTEXT.md }
last_compiled: 86a4e5242ce1f547f13bb0411745db918726a921
---

# Audit Log

## What it does

<!-- [coverage: high -- ADR-0023 read against JdbcAuditFeed, AuditLogAccess and the web section] -->

An oversight Karyakar opens one chronological feed of **who did what**: Occurrences cancelled,
rescheduled, reopened; roles appointed; Sabhas defined; Cities, Zones, Kshetras and Sabha Kinds
created; selections nominated and decided. Each row names the actor, the time, the target and, where
a **Nirikshak acted as a Sanchalak**, the person it was done on behalf of.

**There is no audit table.** Every slice records its own provenance inline on the row it writes, so
the viewer is assembled at read time from five source tables — the capability is the *viewer*, not
the capture. Attendance markings are deliberately outside it: the highest-frequency data in the
system would drown the governance signal.

Reading it is a Nirdeshak-and-above privilege, cut to each reader's own geography.

## Flow

<!-- [coverage: medium -- backend path read end to end including the UNION and the scope predicate; the web half read from the component and its service, not the template] -->

**Web** — one section, one endpoint.

1. The sidebar offers the section only if the session says so, and that flag derives from the same
   engine the BFF uses, not a second tier list.
2. `GET /bff/audit-log` carries the filter bar: target type (a closed enum), action, actor, a
   `from`/`to` calendar range, and the proxy-only toggle. A denial surfaces as a 403 message.
3. An entity screen deep-links in with `?targetType=…&targetId=…`, pinning one entity's history as a
   clearable chip — the same read with two filters set, not a second query path.

**Backend** — `AuditLogBffController` (cookie session, ADR-0022) resolves the caller, asks
`AuditLogAccess` for an `AuditScope`, refuses a `Denied` before touching the database, translates the
inclusive `to` date to an exclusive next-day bound, and calls `AuditLogQueries.find(scope, filter)`.

**Data** — `JdbcAuditFeed` is where the work is. Nine `UNION ALL` branches project the source tables
into one common shape, and **each branch resolves its own geography** by joining out to the structural
tables — an Occurrence through its Sabha, an appointment through whichever scope column it carries, a
structural creation *is* its geography. Everything after that — the scope predicate, the actor-name
join against `users`, the ordering — is uniform.

**Mobile** — `_none_`.

## Rules & authority

<!-- [coverage: high -- AuditLogAccess, AuditScope and the feed's WHERE assembly read directly against ADR-0023] -->

- **Three answers, one sealed type.** `Unrestricted` (Sant, Madhyastha Karyalaya), `Scoped` (a
  Nirdeshak or Sah-Nirdeshak's Kshetras, a Sanyojak's Zones, a Regional Team member's Cities) and
  `Denied`. `AuditLogAccess` is the engine — [authorization](../patterns/authorization.md).
- **The forbidden tiers are never enumerated.** Sanchalak, Sah-Sanchalak and Nirikshak simply hold no
  Nirdeshak-and-above geographic row, so *"resolves to no scope"* folds to `Denied` on its own. The
  tier list exists in ADR-0023's prose and nowhere in the code.
- **The oversight read is broader than the write authority** it mirrors: it is geographic only, never
  demographic-filtered.
- **State-level acts fall out without a special case.** A Sabha Kind creation or an MK-scoped
  appointment resolves to all-`NULL` geography, and `NULL` matches no `IN` list, so only an
  unrestricted caller sees them.
- **The proxy toggle only ever yields Occurrence rows**, because `on_behalf_of_user_id` exists on one
  source table. Correct, not a bug — see [sanchalak-proxy](sanchalak-proxy.md).
- **Ids are not unique across the feed.** One `selection_nominations` row emits both a nominate and a
  decide entry, so a consumer keys on `(id, action)`.
- **Rejections.** `Denied` is **403** and the feed is never queried; a resolved caller with an empty
  result set gets an empty list, which is a different thing and looks different.
- Reading joins other contexts' tables directly, which is the read-model latitude ADR-0029 grants;
  the authority decision above it still goes through ports.

## Where the code is

<!-- [coverage: high -- direct paths, all verified present] -->

- [backend-analytics](../structure/backend-analytics.md) — `AuditLogBffController`, `AuditLogAccess`, the sealed `AuditScope`,
  `AuditLogQueries` / `JdbcAuditFeed`, `JdbcAuditScopeLookup`, `AuditReadAccessAdapter`.
- [backend-common-domain](../structure/backend-common-domain.md) — the `AuditReadAccess` port the nav gate consults, `CallerResolver`,
  `WhereClause`.
- [backend-identity](../structure/backend-identity.md) — `VisibleSections` and the session that carries the flag; `users`, which the
  feed joins for display names.
- [backend-container](../structure/backend-container.md) — `slice-19`, which adds no schema at all: it seeds audit-bearing rows,
  because no earlier seed writes any.
- [web](../structure/web.md) — the `audit-log` section and its filter object.
- The audited rows are [backend-attendance](../structure/backend-attendance.md)'s, [backend-sabha](../structure/backend-sabha.md)'s and
  [backend-identity](../structure/backend-identity.md)'s, written by [occurrence-lifecycle](occurrence-lifecycle.md),
  [role-appointment](role-appointment.md), [sabha-definition](sabha-definition.md), [structural-hierarchy](structural-hierarchy.md) and
  [selection](selection.md).

## Amendments

<!-- [coverage: medium -- reconstructed from the slice-19 changelog header and ADR-0023's own history paragraph; the issue-to-change mapping is inferred] -->

- **Slice 19** (issue #20) — the viewer: the UNION read-model, the engine, the BFF and the web
  section. No write path changed, and no backfill was needed.
- **Issue #80** — the sidebar gate stopped mirroring the tier set as a `Role`-only constant, which
  had silently omitted the Regional Team, and began deriving it through `AuditReadAccess` over the
  same engine. The mirror-versus-derive lesson is [authorization](../patterns/authorization.md)'s.
- **Issue #131** — the read operation is named `listAuditEntries` explicitly, because `list` collided
  with the reopen feed's operation in the generated clients.

## Method

- `JdbcAuditFeed`'s `FEED` constant is the source that paid: the nine branches are the only
  enumeration of what is audited and how each kind reaches a geography, and ADR-0023's table lists
  five sources where the SQL shows nine branches — the structural row is four of them and
  selection is two.
- ADR-0023 is unusually complete for a page like this and carries the reasoning the code cannot: why
  `attendance_markings` is excluded, and why "no scope" and "forbidden tier" are the same answer.
