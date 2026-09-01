---
type: feature
title: Dashboards
description: Attendance analytics read through whichever slice of the organisation the caller is entitled to.
aliases: [dashboard, analytics, KPI strip, People analytics, Sabha analytics, roll-up, Sant universal read, City picker, default city, scope chip]
tags: [bff]
source_paths: [
  apps/backend/analytics-service/*/src/main/**,
  apps/web/src/app/sections/dashboard/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-15/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-17/**,
  docs/adr/0003-*.md,
  docs/adr/0008-*.md,
  docs/adr/0010-*.md,
  docs/adr/0022-*.md,
  CONTEXT.md
]
issues: [16, 18, 66, 73]
sources:
  - { id: adr-0003, title: "Platform Split: Mobile for Sabha-Level Operations, Web for Everything Else", resource: ../../adr/0003-platform-split-by-role.md }
  - { id: adr-0008, title: "Single Bounded Context, with Internal Package Seams", resource: ../../adr/0008-single-bounded-context-with-internal-seams.md }
  - { id: adr-0010, title: "Re-engagement Candidate Definition", resource: ../../adr/0010-re-engagement-candidate-definition.md }
  - { id: adr-0022, title: "Web session via a Backend-for-Frontend with an HTTP-only cookie", resource: ../../adr/0022-web-session-via-bff-http-only-cookie.md }
  - { id: context, title: "CONTEXT.md — Sant, Madhyastha Karyalaya, Regional Team, Nirdeshak, Re-engagement candidate", resource: ../../../CONTEXT.md }
last_compiled: 86a4e5242ce1f547f13bb0411745db918726a921
---

# Dashboards

## What it does

<!-- [coverage: high -- ADR-0010 and CONTEXT.md's Sant entry, read against DashboardAccess, DashboardQueries and the web section] -->

Every signed-in web user lands on a **dashboard**, and every dashboard shows the same three views of
whatever slice of the organisation that person is entitled to:

- **Overview** — a KPI strip (candidates, priority candidates, Sabhas with candidates) plus the
  headline list.
- **People analytics** — the in-scope People one row each, with the Home Sabha they are drifting
  from and their streak.
- **Sabha analytics** — the Zone → Kshetra → Sabha tree with a count at every level, which is the
  roll-up `CONTEXT.md` promises each tier.

What the views *contain* is [re-engagement](re-engagement.md)'s subject; this page is **who sees which rows**, which
is the whole of the capability: one screen, and a scope resolved per caller. A
Sanchalak sees their Sabha, a Nirdeshak their Kshetra × demographic, the Madhyastha Karyalaya
everything — and a **Sant** reads any City in the State regardless of their formal assignment, which
is the one rule here that no role row can express.

Web only, per ADR-0003. There is no dashboard on the phone.

## Flow

<!-- [coverage: medium -- backend path read end to end including the scope SQL; the web half read from the components, not their templates] -->

**Web** — one section with three internal tabs, all reading the same resolved scope.

1. `GET /bff/dashboard/scope` fills the header chip. For a Sant it carries the City list and their
   current pick; for everyone else it comes back inert and the shell renders a static indicator.
2. `GET /bff/dashboard/overview`, `/people` and `/sabha-tree` fill the three tabs. Each is a plain
   read — the caller sends no scope, because the scope is not theirs to state.
3. `POST /bff/dashboard/city` is the Sant's pick. It persists as their default and the section
   remounts, so all three tabs re-read under the new City.
4. `GET`/`PUT /bff/dashboard/thresholds` is the Madhyastha Karyalaya's editor on the overview tab,
   and belongs to [re-engagement](re-engagement.md).

**Backend** — `DashboardBffController` (cookie session, ADR-0022) resolves the Keycloak subject to a
User, hands it to `DashboardAccess`, and passes the resulting `DashboardScope` into `DashboardQueries`.
Both halves are narrow on purpose: the engine decides and reads nothing, and the adapter serves the
decision. `JdbcDashboardQueries` turns each scope case into one WHERE predicate over the candidate
projection joined live to `sabhas → kshetras → zones`.

**Mobile** — `_none_`.

## Rules & authority

<!-- [coverage: high -- DashboardAccess, DashboardScope and JdbcDashboardQueries read directly against ADR-0010 and CONTEXT.md] -->

- **The scope is a sealed three-case answer**, not a filter the client sends: `RoleScoped` (the
  caller's own `role_assignments`), `CityScoped` (a Sant's chosen City) and `NoCity`. `DashboardAccess`
  is the engine — [authorization](../patterns/authorization.md), where it is the deviation that reads no roles at all.
- **A role-scoped read is the canonical visibility predicate**, `CallerVisibility`, shared with the
  Occurrence-reopen list. The dashboard grants every tier, and the grant is an **opt-in enum set**:
  a tier added elsewhere is not silently given dashboard visibility.
- **A Nirikshak resolves through their explicit proxy assignment**, not their Kshetra role row —
  the same asymmetry [sanchalak-proxy](sanchalak-proxy.md) turns on.
- **A Sant who has not picked a City sees nothing.** `NoCity` compiles to `1 = 0` and the web shows a
  prompt rather than empty sections; the City *is* the default, so picking and remembering are one act.
- **Rejections.** A non-Sant calling the City picker is **403** (`NotASantException`); an unknown City
  is **404**; an authenticated subject with no local User is **403**, not a server error.
- **The Sant's chosen City is analytics' column on identity's `users` table** — the backend's only
  cross-context table write, made deliberately because the preference is a dashboard concern and
  nothing in identity reads it.

## Where the code is

<!-- [coverage: high -- direct paths, all verified present] -->

- [backend-analytics](../structure/backend-analytics.md) — `DashboardBffController`, `DashboardAccess`, the sealed `DashboardScope`,
  `DashboardQueries` and `JdbcDashboardQueries`, `JdbcSantDefaultCity`, `JdbcCityDirectory`.
- [backend-common-domain](../structure/backend-common-domain.md) — `CallerVisibility` and `VisibilityTier`, `SantLookup`,
  `MadhyasthaKaryalayaLookup`, `CallerResolver`, `WhereClause`.
- [backend-identity](../structure/backend-identity.md) — the `users` row the default City hangs off, and the Sant lookup's adapter.
- [backend-container](../structure/backend-container.md) — `slice-17`'s `users.default_city_id` and the Sant seed; `slice-15`'s
  projection the reads are served from.
- [web](../structure/web.md) — the `dashboard` section: the tab shell, the three view components, the City chip.

## Amendments

<!-- [coverage: medium -- reconstructed from the slice-15/slice-17 changelog headers and class javadocs; the issue-to-change mapping is inferred] -->

- **Slice 15** (issue #16) — the dashboard itself: three sections, read from the candidate
  projection, scoped by the caller's role assignments.
- **Slice 17** (issue #18) — the Sant landing. `DashboardScope` became sealed and three-cased, the
  City chip and `users.default_city_id` arrived, and the pre-existing behaviour was renamed
  `RoleScoped` rather than special-cased.
- **Issue #66** — the role-scope SQL and the Nirikshak's proxy branch moved into `CallerVisibility`,
  shared with the reopen read model; the two files used to warn each other to keep in step.
- **Issue #73** — the web reads through the generated typed client rather than a hand-written service.

## Method

- `JdbcDashboardQueries` is the source that paid. `GRANTED_TIERS` and the three scope predicates are
  the only statement anywhere of *which* tiers the dashboard admits and that the Nirikshak resolves
  through the proxy assignment instead of their role row — ADR-0010 says "every tier with scope" and
  stops there.
- The Sant rules needed both ends: `CONTEXT.md` gives the universal read and the default City,
  `DashboardAccess` gives what happens before the first pick, and neither implies the other.
