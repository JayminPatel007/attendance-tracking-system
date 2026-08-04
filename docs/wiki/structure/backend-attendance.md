---
kind: structure
slug: backend-attendance
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
decisions: [ADR-0001, ADR-0007, ADR-0012, ADR-0019, ADR-0021, ADR-0028]
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

The standard five-module ring. 74 main source files — the second-largest backend unit, and unusually
top-heavy: 47 of them sit in the use-case ring.

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
higher-tier interventions (reopen, Nirikshak proxy) are `/bff/*`.

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

- [[attendance-marking]]

Occurrence lifecycle (open / finalize / cancel / reschedule / reopen / proxy) is a distinct
capability and a dossier candidate; it is deliberately **not** folded into the marking dossier.

## Sources

- [ADR-0001](../../adr/0001-sabha-occurrence-lifecycle.md), [ADR-0007](../../adr/0007-offline-capable-attendance-marking.md), [ADR-0012](../../adr/0012-sabha-schedule-shapes-and-occurrence-materialization.md), [ADR-0019](../../adr/0019-bounded-context-module-taxonomy.md), [ADR-0021](../../adr/0021-spring-scheduling-for-occurrence-cron.md), [ADR-0028](../../adr/0028-jdbcclient-persistence-and-in-aggregate-lifecycles.md)
- [CONTEXT.md](../../../CONTEXT.md) — Sabha Occurrence, Attendance Marking, Walk-in, Sanchalak, Nirikshak
- `Occurrence.java`, `OccurrenceWriter.java`, `MarkAttendanceApplicationService.java` — javadoc, the highest-yield source in this unit
