---
type: structure
title: Attendance Service
description: Owns Sabha Occurrences and the markings against them, including the offline sync path and walk-in capture.
resource: apps/backend/attendance-service
aliases: [Occurrences and markings, Sabha Occurrence, Roster, Walk-in]
tags: [offline-sync]
source_paths: [
  apps/backend/attendance-service/*/src/main/**,
  apps/backend/attendance-service/*/pom.xml,
  apps/backend/attendance-service/pom.xml,
  docs/adr/0001-*.md,
  docs/adr/0007-*.md,
  docs/adr/0012-*.md,
  docs/adr/0019-*.md,
  docs/adr/0021-*.md,
  docs/adr/0028-*.md,
  CONTEXT.md
]
sources:
  - { id: adr-0001, title: "Sabha Occurrence Lifecycle", resource: ../../adr/0001-sabha-occurrence-lifecycle.md }
  - { id: adr-0007, title: "Mobile App is Offline-Capable for Attendance Marking Only", resource: ../../adr/0007-offline-capable-attendance-marking.md }
  - { id: adr-0012, title: "Sabha Schedule Shapes and Occurrence Materialization", resource: ../../adr/0012-sabha-schedule-shapes-and-occurrence-materialization.md }
  - { id: adr-0019, title: "Bounded-context module taxonomy: five modules per context, presentation split from application service", resource: ../../adr/0019-bounded-context-module-taxonomy.md }
  - { id: adr-0021, title: "Spring Scheduling for Occurrence cron jobs", resource: ../../adr/0021-spring-scheduling-for-occurrence-cron.md }
  - { id: adr-0028, title: "Persistence stays on JdbcClient (no JPA); aggregate lifecycles stay in-aggregate (no Spring State Machine)", resource: ../../adr/0028-jdbcclient-persistence-and-in-aggregate-lifecycles.md }
  - { id: context, title: "CONTEXT.md — Sabha Occurrence, Attendance Marking, Walk-in, Sanchalak, Nirikshak", resource: ../../../CONTEXT.md }
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---

# Attendance Service

## Purpose

<!-- [coverage: high -- class listing + ADR-0001/0007/0012] -->

Owns the **Sabha Occurrence** — one dated instance of a Sabha — its state machine, and the
**Attendance Markings** recorded against it. Everything that materializes occurrences (the weekly
and monthly scanners), moves them through `Scheduled ↔ Rescheduled | Cancelled → Open for Marking →
Finalized` (ADR-0001), and accepts markings online or from the mobile offline queue (ADR-0007) lives
here.

This is where the product's namesake capability actually runs.

## Layout

<!-- [coverage: medium -- directory + class listing only; every package-info.java here is an empty ADR-0019 scaffold] -->

The standard five-module [module-ring](../patterns/module-ring.md). 74 main source files — the
second-largest backend unit, and unusually top-heavy: 47 of them sit in the use-case ring.

| Module | Main files | Holds |
|---|---|---|
| `attendance-domain/attendance-domain-core` | 12 | `Occurrence` (the aggregate), `AttendanceMarking`, `OccurrenceState`, `MarkingType`, four domain events (`AttendanceMarked`, `OccurrenceOpened`, `OccurrenceFinalized`, `OccurrenceReopened`) and the transition exceptions. |
| `attendance-domain/attendance-application-service` | 47 | `OccurrenceWriter`, `MarkAttendanceApplicationService`, `SyncAttendanceApplicationService`, `OccurrenceShapingService`, `OccurrenceReopenService`, `AuthorizationEngine`, the three scanners, and the query ports + DTOs. |
| `attendance-data-access` | 10 | 9 `Jdbc*` adapters (ADR-0028: `JdbcClient`, in-aggregate lifecycles). |
| `attendance-application` | 4 | 3 REST controllers (ADR-0017). |
| `attendance-messaging` | 1 | `package-info.java` only — empty scaffold; this unit sends nothing. |

The one grouping worth knowing inside the use-case ring: **`OccurrenceWriter` is the single owner of
the load-retry-save-publish cycle**. Every other writer — marking, shaping, reopen, the scanners —
goes through it rather than touching `OccurrenceRepository` directly.

## Exposes

<!-- [coverage: high -- mapping-annotation grep over attendance-application] -->

3 controllers, and the only backend unit whose surface is **substantially mobile**.

| Prefix | Serves | Controllers |
|---|---|---|
| `/api/sanchalak/*` | mobile | `AttendanceRestController` — current roster, current occurrence, monthly sabhas |
| `/api/occurrences/{id}/*` | mobile | `AttendanceRestController` — markings, walk-ins, cancel, revert, reschedule, venue-override |
| `/api/sync`, `/api/sabhas/{id}/*` | mobile | `AttendanceRestController` — the offline push, ad-hoc occurrence creation, monthly compliance |
| `/bff/occurrences/*` | web | `OccurrenceReopenBffController` |
| `/bff/proxy/*` | web | `SanchalakProxyBffController` |

The split is ADR-0003 in one table: the Sanchalak's capture surface is `/api/*`, and the two
higher-tier interventions (reopen, Nirikshak proxy) are `/bff/*`. Which tier may do which is
`AuthorizationEngine`'s call — see [authorization](../patterns/authorization.md).

## Talks To

<!-- [coverage: high -- import scan of all four modules] -->

Zero direct imports of another context's packages. Attendance is the backend's **pure consumer** —
it implements no common-domain port for anyone.

**Outbound**:

| Port | Target | Used by |
|---|---|---|
| `CallerResolver` | identity | almost everything — 14 references, the most of any unit |
| `RoleAssignmentLookup` | identity | `AuthorizationEngine` |
| `NirikshakAssignmentLookup` | identity | `AuthorizationEngine` (the proxy check) |
| `UserActivityRecorder` | identity | `SyncAttendanceApplicationService` |
| `StructuralHierarchyLookup` | sabha | `AuthorizationEngine` (reopen scope), the queries |
| `SabhaScheduleLookup`, `SabhaShapeLookup`, `WeeklySabhaCatalog` | sabha | the materialization scanners |
| `DomainEventPublisher` | application-container | `OccurrenceWriter` |

**Inbound** — `_none_`. Nothing depends on this unit through common-domain; analytics reads its
tables directly instead (see Data).

## Data

<!-- [coverage: medium -- writer grep across all four contexts; ownership inferred from writers, no ownership manifest exists] -->

Migrations are central (`protocol.md` §8) and outside this page's `source_paths`.

**Owned** (attendance is the only writer): `occurrences`, `attendance_markings`,
`occurrence_state_transitions`.

**Read but not owned**: `sabhas`, `home_sabhas`, `persons`, `users`, `role_assignments`, `kshetras`,
`nirikshak_sabha_assignments` — the roster and authority reads, all identity's or sabha's.

`occurrence_state_transitions` is append-only and is what makes the "reopened" badge derivable
rather than stored: the badge is computed from the transition history, not from a column on
`occurrences`. `occurrences` carries a `version` column for the optimistic lock `OccurrenceWriter`
retries on.

## Gotchas

<!-- [coverage: medium -- read from OccurrenceWriter/MarkAttendance javadoc and the aggregate; no exhaustive sweep] -->

- **Markings take the writer's *unaudited* path.** `MarkAttendanceApplicationService` deliberately
  appends no `occurrence_state_transitions` row, because marking changes no lifecycle state; each
  marking carries its own `markedBy` instead. Don't look for markings in the transition history.
- **`@Transactional` on the writer is load-bearing for the cron path**, not decoration — the
  scanners have no surrounding transaction of their own.
- The aggregate exposes both `markings()` and a separate drained set of *pending* markings.
  Repositories iterate the pending set so a save writes only mutated rows; iterating `markings()`
  instead would rewrite the whole roster on every marking.

## Covered by

<!-- [coverage: high -- one dossier exists and it names this unit] -->

- [attendance-marking](../features/attendance-marking.md)

Occurrence lifecycle (open / finalize / cancel / reschedule / reopen / proxy) is a distinct
capability and a dossier candidate; it is deliberately **not** folded into the marking dossier.

## Method

- Javadoc on `Occurrence.java`, `OccurrenceWriter.java` and `MarkAttendanceApplicationService.java` — the highest-yield source in this unit, and unusually substantive for this repo.
- Ring-module class listings, a mapping-annotation grep for `Exposes`, and a writer-SQL grep for `Data` → Owns.
