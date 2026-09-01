---
type: structure
title: Analytics Service
description: The read-side context: dashboards, the audit-log read model and the re-engagement candidate projection.
resource: apps/backend/analytics-service
aliases: [dashboards, audit log, re-engagement, Nirdeshak/Sant reporting]
tags: [audit, bff]
source_paths: [
  apps/backend/analytics-service/*/src/main/**,
  apps/backend/analytics-service/*/pom.xml,
  apps/backend/analytics-service/pom.xml,
  docs/adr/0010-*.md,
  docs/adr/0019-*.md,
  docs/adr/0023-*.md,
  docs/adr/0027-*.md,
  CONTEXT.md
]
sources:
  - { id: adr-0010, title: "Re-engagement Candidate Definition", resource: ../../adr/0010-re-engagement-candidate-definition.md }
  - { id: adr-0019, title: "Bounded-context module taxonomy: five modules per context, presentation split from application service", resource: ../../adr/0019-bounded-context-module-taxonomy.md }
  - { id: adr-0023, title: "Audit log is a read-model over existing tables, viewable by Nirdeshak and above within scope", resource: ../../adr/0023-audit-log-read-model-and-viewer-authority.md }
  - { id: adr-0027, title: "No shared granted-scope module behind the four authorization engines", resource: ../../adr/0027-no-shared-granted-scope-module-behind-the-authorization-engines.md }
  - { id: context, title: "CONTEXT.md — Sant, Madhyastha Karyalaya, Re-engagement Candidate", resource: ../../../CONTEXT.md }
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---

# Analytics Service

## Purpose

<!-- [coverage: high -- class listing + ADR-0010/0023] -->

The **read side**. Owns the higher-tier dashboards, the audit-log viewer (ADR-0023) and the
re-engagement candidate model (ADR-0010) — who has drifted out of attendance and needs following up.
It also owns the two pieces of configuration that drive those reads: the re-engagement thresholds
and a Sant's default City.

Almost everything it reports on is somebody else's data. It is the one unit whose job is to read
across context boundaries.

## Layout

<!-- [coverage: medium -- directory + class listing only; every package-info.java here is an empty ADR-0019 scaffold] -->

The standard five-module [module-ring](../patterns/module-ring.md). 48 main source files.

| Module | Main files | Holds |
|---|---|---|
| `analytics-domain/analytics-domain-core` | 8 | `Candidate`, `Thresholds`, `Tier`, `Scope`, `OutcomeKind`, `HomeSabhaHistory` — the calculator's vocabulary. |
| `analytics-domain/analytics-application-service` | 26 | `ReEngagementCandidateCalculator` (DB-free, two ports), `ReEngagementProjectionScanner`, `DashboardAccess`, `DashboardQueries`, `AuditLogAccess`, `AuditLogQueries`, `AuditReadAccessAdapter`. |
| `analytics-data-access` | 10 | 9 `Jdbc*` adapters, including the cross-context reads. |
| `analytics-application` | 3 | 2 BFF controllers (ADR-0017). |
| `analytics-messaging` | 1 | `package-info.java` only — empty scaffold; this unit sends nothing. |

Two authorization engines live side by side here — `DashboardAccess` and `AuditLogAccess` — kept
separate rather than unified, consistent with ADR-0027's refusal of a shared granted-scope module.
Both are instances of [authorization](../patterns/authorization.md).

## Exposes

<!-- [coverage: high -- mapping-annotation grep over analytics-application] -->

2 controllers, **all `/bff/*`** — no mobile surface at all. Analytics is a web-tier capability by
ADR-0003.

| Prefix | Serves | Controllers |
|---|---|---|
| `/bff/dashboard/*` | web | `DashboardBffController` — overview, people, sabha-tree, scope, city, thresholds (GET + PUT) |
| `/bff/audit-log` | web | `AuditLogBffController` |

## Talks To

<!-- [coverage: high -- import scan of all four modules] -->

Zero direct imports of another context's packages — but see Data: this unit's real coupling to the
others is through **SQL**, not through Java.

**Outbound**:

| Port | Target | Used by |
|---|---|---|
| `SantLookup` | identity | `DashboardAccess`, `AuditLogAccess` — the Sant universal-read rule |
| `MadhyasthaKaryalayaLookup` | identity | `DashboardAccess`, `ThresholdAdmin` |
| `CallerResolver` | identity | both controllers |

**Inbound** — one port: `AuditReadAccess`, implemented by `AuditReadAccessAdapter`, which lets other
contexts ask whether a caller may read audit data without depending on analytics.

## Data

<!-- [coverage: medium -- writer grep across all four contexts; ownership inferred from writers, no ownership manifest exists] -->

**Owned**: `analytics_thresholds`, `reengagement_candidates` (the background projection ADR-0010's
calculator feeds).

**Also writes, but does not own**: `users.default_city_id`, via `JdbcSantDefaultCity`. This is the
backend's only cross-context table write — analytics owns that one *column* on identity's table.
Treat it as a known, deliberate exception rather than a pattern to copy.

**Read but not owned** — the widest read surface in the backend: `persons`, `users`,
`role_assignments`, `selection_nominations` (identity); `cities`, `zones`, `kshetras`, `sabhas`,
`sabha_kinds` (sabha); `occurrences`, `attendance_markings`, `occurrence_state_transitions`
(attendance); `home_sabhas`.

The audit log is a **CQRS UNION read-model** per ADR-0023 — there is no central audit table. It is
assembled at query time from the other contexts' tables, which is precisely why this unit's read
list is so long.

## Gotchas

<!-- [coverage: medium -- one verified from ADR-0023 and the query classes; no exhaustive sweep] -->

- **Don't go looking for an `audit_log` table** — there isn't one, by ADR-0023. `JdbcAuditFeed`
  UNIONs across the owning contexts' tables at read time.
- `ReEngagementCandidateCalculator` is deliberately **DB-free** — it takes two ports and no
  connection. Its unit tests need no database, and adding a query to it would forfeit that.
- Analytics reads other contexts' tables directly rather than through ports. That is legal read-model
  latitude under ADR-0029, but it means a rename in someone else's schema breaks *this* unit's SQL
  with no compile error anywhere.

## Covered by

<!-- [coverage: high -- derived: the three dossiers below name this page in their `Where the code is`] -->

- [dashboards](../features/dashboards.md) — the scoping engine and the three scoped reads.
- [audit-log](../features/audit-log.md) — the UNION feed, its engine and the BFF over them.
- [re-engagement](../features/re-engagement.md) — the calculator, the thresholds and the projection.

Three of three: every capability this unit holds is now covered, which is what a single-purpose read
context should look like.

## Method

- Class listing plus a writer/reader SQL grep over `analytics-service/**`. The reader half matters more here than on any other page: this is the one context whose job is querying other people's tables, so `Data` → Reads is long and Owns is short.
- All five `package-info.java` files are ADR-0019 ring scaffolds and were skipped.
