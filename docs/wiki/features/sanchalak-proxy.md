---
type: feature
title: Sanchalak Proxy
description: A Nirikshak exercising the Sanchalak's toolkit on an assigned Sabha when the Sanchalak is unavailable.
aliases: [proxy mode, acting as Sanchalak, Nirikshak, on behalf of, last seen, assigned Sabhas, stand-in]
tags: [audit, bff]
source_paths: [
  apps/backend/attendance-service/*/src/main/**,
  apps/backend/common-domain/src/main/java/org/sabha/common/AuthorizedAction.java,
  apps/web/src/app/sections/sanchalak-proxy/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-14/**,
  docs/adr/0001-*.md,
  docs/adr/0003-*.md,
  CONTEXT.md
]
issues: [15]
sources:
  - { id: adr-0001, title: "Sabha Occurrence Lifecycle", resource: ../../adr/0001-sabha-occurrence-lifecycle.md }
  - { id: adr-0003, title: "Platform Split: Mobile for Sabha-Level Operations, Web for Everything Else", resource: ../../adr/0003-platform-split-by-role.md }
  - { id: context, title: "CONTEXT.md — Nirikshak, Sanchalak, Sabha Occurrence", resource: ../../../CONTEXT.md }
last_compiled: 725c3bb2acc25b0d6eca106747727b427695b0b1
---

# Sanchalak Proxy

## What it does

<!-- [coverage: high -- CONTEXT.md's Nirikshak entry and ADR-0001, read against AuthorizationEngine and SanchalakProxyBffController] -->

A **Nirikshak** oversees three or four Sabhas within a Kshetra. On any Sabha assigned to them, they
may step into the **Sanchalak's** operational toolkit when the Sanchalak is unavailable — cancelling,
rescheduling or re-venuing an Occurrence, exactly as the Sanchalak would.

The point of the capability is that it is a **stand-in, not a tier-skip**. A Nirdeshak or Sanyojak who
needs a Sabha's Occurrence changed does not reach past the chain to do it; they route through the
Nirikshak, and ADR-0001 says so explicitly.

The set of Sabhas is an **explicit, mutable assignment** made by the Nirdeshak — the group has no
name or identity beyond *"the Sabhas currently assigned to Nirikshak X"*. Every proxy action is
audited as the acting Nirikshak, with the absent Sanchalak recorded beside them.

The actions themselves are [occurrence-lifecycle](occurrence-lifecycle.md)'s; this page is who may borrow them, and how it
is recorded.

## Flow

<!-- [coverage: medium -- backend path read end to end, including the picker SQL; the web half read from the component, not its template] -->

**Web** — the only surface, and deliberately so: the Nirikshak is a web-tier role under ADR-0003
while the Sanchalak works from the phone, so the same operations are reached from different apps.

1. `GET /bff/proxy/sabhas` returns the picker — the Sabhas assigned to the caller, each with the
   Sanchalak's name and a **"last seen"** hint. The hint is informational and never gates entry.
2. Entering a Sabha switches the section into *acting* mode, behind a prominent banner declaring that
   the Nirikshak is acting as its Sanchalak and that every action is logged as them.
3. `GET /bff/proxy/sabhas/{id}/occurrences` lists what may still be shaped — `SCHEDULED` and
   `RESCHEDULED` only — and `POST /bff/proxy/occurrences/{id}/cancel|reschedule|venue-override`
   performs the act.

**Backend** — the proxy endpoints are a thin second door onto the Sanchalak's own service:

- `SanchalakProxyBffController` delegates straight to `OccurrenceShapingService`, so the proxy and the
  Sanchalak cannot drift apart.
- `AuthorizationEngine` grants a shaping action when the caller is the Sabha's Sanchalak **or** an
  assigned Nirikshak, and its `onBehalfOf` returns the absent Sanchalak when — and only when — the
  caller is exercising the proxy rather than acting as the Sanchalak themselves.
- `OccurrenceWriter` stamps both ids onto the audit row.
- `JdbcProxySabhaQueries` builds the picker as a CQRS read straight over the schema, taking "last
  seen" as the `GREATEST` of the Sanchalak's last login, last sync and last attendance marking.

**Mobile** — `_none_`.

## Rules & authority

<!-- [coverage: high -- AuthorizationEngine, AuthorizedAction.SABHA_SHAPING_ACTIONS and the slice-14 migration read directly against ADR-0001 and CONTEXT.md's Nirikshak entry] -->

- **Scope is the explicit assignment, not the role.** `nirikshak_sabha_assignments` is what the engine
  checks; a Nirikshak is refused on any Sabha outside it, even inside their own Kshetra. This is a
  different scope from the `(Kshetra, demographic)` role row that grants the same person the
  Occurrence reopen — [authorization](../patterns/authorization.md).
- **The proxy borrows the shaping set, and only it.** `SABHA_SHAPING_ACTIONS` is what the engine
  grants an assigned Nirikshak; the web toolkit surfaces three. It is not a second reopen
  path; reopen resolves through the role row.
  **Three claims, not a conflict:** CONTEXT.md's Nirikshak entry also lists five proxy powers, and it
  is a *different* five. Cancel, reschedule and standing-schedule change are in both;
  `VENUE_OVERRIDE` and `CREATE_OCCURRENCE` are in the set and not in CONTEXT.md; and CONTEXT.md's
  marking, walk-ins and directory-add are outside the set entirely — `canUserDo` returns `false` for
  anything that is neither shaping nor `REOPEN`, so whatever grants those three, it is not this
  engine. So: five in the set, five in CONTEXT.md, three in the toolkit — the last being the shipped
  surface, not the granted one.
- **Attribution is two ids, never one.** `actor_user_id` is the Nirikshak who acted;
  `on_behalf_of_user_id` is the Sanchalak it was done for. A Sanchalak acting on their own Sabha
  leaves the second null, so *"find all proxy actions"* is a partial index over non-null.
- **Rejections.** An unassigned Sabha, or a caller who is neither Sanchalak nor assigned Nirikshak →
  **403**. The shaping preconditions — the cancel reason, the revert window, the state guards — are
  the same ones the Sanchalak meets, refused the same way.
- **"Last seen" is a hint, not a gate.** It may be absent entirely, and an absent Sanchalak signal
  simply drops out of the `GREATEST`.

## Where the code is

<!-- [coverage: high -- direct paths, all verified present] -->

- [backend-attendance](../structure/backend-attendance.md) — `SanchalakProxyBffController`, the `AuthorizationEngine`'s proxy branch and
  `onBehalfOf`, `JdbcProxySabhaQueries`, and the shaping service they share.
- [backend-identity](../structure/backend-identity.md) — `JdbcNirikshakAssignmentLookup`, the adapter that answers *is this Sabha
  assigned to this Nirikshak?*, plus the `user_activity` rows the hint reads.
- [backend-common-domain](../structure/backend-common-domain.md) — `NirikshakAssignmentLookup`, `UserActivityRecorder`, `CallerResolver`.
- [backend-container](../structure/backend-container.md) — `slice-14`'s `nirikshak_sabha_assignments`, the
  `occurrence_state_transitions.on_behalf_of_user_id` column and its partial index, and `user_activity`.
- [web](../structure/web.md) — the `sanchalak-proxy` section.

## Amendments

<!-- [coverage: medium -- reconstructed from the slice-14 changelog header and class javadocs; the issue-to-change mapping is inferred. The no-production-writer claim is high: a writer grep over all four contexts leaves only the seed] -->

- **Slice 14** (issue #15) — the whole capability: the explicit assignments table, the on-behalf-of
  audit column, `user_activity`, the picker projection and the BFF toolkit.
- **Issue #128** — the on-behalf-of resolution moved into `OccurrenceWriter`'s single authorize step,
  so proxy attribution now happens on the same path every other Occurrence write takes.

**Worth knowing:** nothing in production writes `nirikshak_sabha_assignments`. The only rows come
from `slice-14/002-seed.sql`, so the assignment the Nirdeshak is supposed to make has no surface yet
— the capability is reachable in a seeded environment and not otherwise.

## Method

- `AuthorizationEngine.onBehalfOf` is the source that paid: its three conditions are the entire
  distinction between a Sanchalak acting and a Nirikshak proxying, and neither ADR-0001 nor
  `CONTEXT.md` says how the two are told apart at write time.
- `slice-14/001-sanchalak-proxy.sql`'s header is unusually load-bearing for a migration — it names all
  three structural additions and, decisively, distinguishes the assignment table from the
  Kshetra-tier role row. Read it before the Java on any recompile.
