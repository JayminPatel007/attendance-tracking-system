---
kind: structure
slug: backend-attendance
source_paths: [apps/backend/attendance-service/**]
decisions: [ADR-0001, ADR-0007, ADR-0012, ADR-0019, ADR-0020, ADR-0021, ADR-0028]
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---
# Backend — Attendance Context

## Purpose

<!-- [coverage: high] -->

The dated gatherings and who was at them: the `Occurrence` aggregate, its lifecycle
(Scheduled → Open for Marking → Finalized, with Cancelled/Rescheduled/reopen paths per ADR-0001),
Attendance Markings including walk-ins, offline sync from mobile (ADR-0007), and the Nirikshak
proxy surface. This is the only context with a genuine aggregate-with-events model.

## Layout

<!-- [coverage: high] -->

Five Maven modules per ADR-0019:

| Module | Ring | What lives in it |
|---|---|---|
| `attendance-domain-core` | Entities | `Occurrence` (aggregate root), `AttendanceMarking`, `OccurrenceState`, `MarkingType`, and four domain events (`AttendanceMarked`, `OccurrenceOpened`, `OccurrenceFinalized`, `OccurrenceReopened`). Pure Java per ADR-0020. |
| `attendance-application-service` | Use cases | The largest module: four-step application services (load → mutate → save → publish), the cron scanners, `AuthorizationEngine`, `EffectiveSlotResolver`, `OccurrenceWriter`, and the driven ports. Spring dependency limited to `spring-context` + `spring-tx`. |
| `attendance-data-access` | Interface adapters | Nine `Jdbc*` adapters — repositories plus read-side projections that join identity + sabha + attendance tables. |
| `attendance-messaging` | Interface adapters | Empty scaffold. |
| `attendance-application` | Interface adapters | `AttendanceRestController`, `OccurrenceReopenBffController`, `SanchalakProxyBffController`. |

The navigation axis here is the **cluster**, not a package: writes go through `OccurrenceWriter`;
time resolution through `EffectiveSlotResolver`; scheduled transitions through `AutoOpenScanner`,
`AutoFinalizeScanner` and `WeeklyMaterializationScanner`.

## Exposes

<!-- [coverage: high] -->

**`/api/*` — mobile:** `/api/occurrences/{id}/*` (cancel, reschedule, revert, venue-override,
markings, walk-ins), `/api/sabhas/{id}/occurrences`, `/api/sabhas/{id}/monthly-compliance`,
`/api/sanchalak/current-occurrence`, `/api/sanchalak/current-roster`, `/api/sanchalak/monthly-sabhas`,
and `/api/sync` — the offline reconciliation endpoint.

**`/bff/*` — web:** `/bff/occurrences` (+ `/{id}/reopen`) and the `/bff/proxy/*` family a Nirikshak
uses to act on a Sanchalak's behalf.

## Talks To

<!-- [coverage: medium -- edges derived from `import org.sabha.common.*`; no call-graph analysis, and this context imports the widest set of common-domain types of any. ] -->

**Outbound** — into [[backend-identity]] for authority (`CallerResolver`, `RoleAssignmentLookup`,
`NirikshakAssignmentLookup`, `UserActivityRecorder`, `CallerVisibility`) and into [[backend-sabha]]
for shape and schedule (`SabhaShapeLookup`, `SabhaScheduleLookup`, `WeeklySabhaCatalog`,
`StructuralHierarchyLookup`).

**Inbound** — none. No other context depends on an attendance-implemented common-domain port; this
is a pure consumer. That asymmetry is the mirror image of [[backend-identity]]'s.

The cron entry points that drive the scanners live outside this context, in [[backend-container]]
(ADR-0021).

## Data

<!-- [coverage: low -- ownership inferred from INSERT/UPDATE targets; the read-side projections join five other contexts' tables, so a naive grep overstates what this context owns. ] -->

Written here: `occurrences`, `attendance_markings`, `occurrence_state_transitions`.

Read-only, via the roster and proxy projections: `sabhas`, `persons`, `users`, `home_sabhas`,
`role_assignments`, `kshetras`, `nirikshak_sabha_assignments`, `user_activity`.

## Gotchas

<!-- [coverage: medium -- the concurrency posture is stated in package-info.java and the class names; the exact retry policy was not read. ] -->

This is the only context with optimistic locking: `OccurrenceRepository` detects a version conflict
on update and the caller sees `OptimisticLockException` / `ConcurrentModificationException`. Offline
sync and the cron scanners both write the same aggregate, so any new write path must go through
`OccurrenceWriter` rather than saving an `Occurrence` directly — that class owns retry, audit
stamping and event publication together.

## Covered by

<!-- [coverage: low -- no feature dossiers exist yet; structurally empty until the first one is admitted. ] -->

_none_

## Sources

- [attendance-service](../../../apps/backend/attendance-service) — module layout, `package-info.java` in every ring, class inventory
- [ADR-0001](../../adr/0001-sabha-occurrence-lifecycle.md), [ADR-0007](../../adr/0007-offline-capable-attendance-marking.md), [ADR-0020](../../adr/0020-aggregate-root-and-domain-events.md), [ADR-0021](../../adr/0021-spring-scheduling-for-occurrence-cron.md)
- [CONTEXT.md](../../../CONTEXT.md) — Sabha Occurrence, Effective Slot, Occurrence States vocabulary
