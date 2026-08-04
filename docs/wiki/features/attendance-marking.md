---
kind: feature
slug: attendance-marking
source_paths: [
  apps/backend/attendance-service/*/src/main/**,
  apps/mobile/sabha_attendance/lib/roster/**,
  apps/mobile/sabha_attendance/lib/sync/**,
  apps/mobile/sabha_attendance/lib/walk_in/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-2/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-4/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-7/**,
  docs/adr/0001-*.md,
  docs/adr/0003-*.md,
  docs/adr/0007-*.md,
  docs/adr/0013-*.md,
  CONTEXT.md
]
decisions: [ADR-0001, ADR-0003, ADR-0007, ADR-0013]
issues: [3, 5]
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---

# Attendance Marking

## What it does

<!-- [coverage: high -- ADR-0007, ADR-0003 and the mobile roster/sync/walk_in sources] -->

A **Sanchalak** or **Sah-Sanchalak** records who was present at a **Sabha Occurrence**, from the
mobile app, during the gathering. Two marking vocabularies: a **Roster** marking (a Person whose
Home Sabha this is — present or absent) and a **Walk-in** (a Person attending a Sabha that is not
one of their Home Sabhas — always present).

Marking works **offline** — this is the one capability in the product that does (ADR-0007). The
Roster is cached on first sync, markings queue in a local SQLite store, and the queue is pushed when
connectivity returns.

## Flow

<!-- [coverage: medium -- backend path read end to end; mobile path read from the api/controller/engine files, not from the widgets] -->

**Mobile** — the only capture surface. Per ADR-0003 there is deliberately **no web marking UI**;
web's involvement in this capability is limited to the higher-tier reopen that makes a Finalized
Occurrence markable again.

1. `GET /api/sanchalak/current-roster` and `/current-occurrence` populate `AttendanceStore`
   (sqflite), which caches the Roster plus a `rosterVersion`.
2. `roster_screen` marks against the local store. Online or offline, the marking lands in the local
   queue first.
3. `SyncEngine.syncNow()` pushes the queue to `POST /api/sync` with the cached `rosterVersion`. On
   success the queue is cleared and `lastSyncedAt` recorded; on `ROSTER_STALE` the queue is
   **preserved** so the user can refresh and retry rather than lose markings.
4. A Walk-in takes a different route: `GET /api/directory/walk-in-search` then
   `POST /api/occurrences/{id}/walk-ins` — **online only**, never queued.

**Backend** — `attendance-application` → `attendance-application-service` → the `Occurrence`
aggregate:

- `AttendanceRestController` receives both the single-marking and the sync call.
- `SyncAttendanceApplicationService` checks roster age, groups items **by Occurrence**, and calls
  `MarkAttendanceApplicationService.executeBatch` once per Occurrence — so N items against one
  Occurrence cost one load and one save, not N.
- `MarkAttendanceApplicationService` owns only the marking vocabulary (Roster vs Walk-in). The
  load-retry-save-publish cycle is `OccurrenceWriter`'s, taken on its **unaudited** path.
- `Occurrence.record()` enforces the state guard and last-write-wins, and registers an
  `AttendanceMarked` domain event.

**Web** — `_none_` for marking itself. See [[backend-attendance]] for the reopen and proxy surfaces.

## Rules & authority

<!-- [coverage: high -- ADR-0001/0007 cross-checked against Occurrence.java, AuthorizationEngine and SyncAttendanceApplicationService] -->

- **Who.** Sanchalak and Sah-Sanchalak of the Sabha. Sah-Sanchalak is included here and excluded
  from cancel/reschedule — marking is the day-of workload the role exists to share (ADR-0001).
- **When.** Only while the Occurrence is `OPEN_FOR_MARKING`. Any other state throws
  `OccurrenceNotOpenForMarkingException`. `FINALIZED` is set automatically 24 hours after the
  scheduled end time; after that only a Nirikshak / Nirdeshak / Sah-Nirdeshak reopen reopens the
  window.
- **Roster staleness.** The server rejects a sync whose `clientRosterVersion` is older than
  **7 days** — `MAX_ROSTER_AGE` in `SyncAttendanceApplicationService`, matching ADR-0007's
  server-enforced contract. Rejection is `StaleRosterException` → HTTP **409** with
  `code: ROSTER_STALE`.
- **Conflict resolution.** Last-write-wins per `(Occurrence, Person)`, keyed on the client-side
  `clientMarkedAt`; server arrival time is a tiebreaker only. Two Karyakars marking the same Person
  is expected and non-substantive, so there is no conflict UI.
- **Concurrency.** `occurrences` carries a `version` column; `OccurrenceWriter` retries on
  optimistic-lock conflict, which is what makes concurrent batches safe.
- **Not audited.** Marking appends **no** `occurrence_state_transitions` row — it changes no
  lifecycle state. Attribution rides each marking's own `markedBy`.

## Where the code is

<!-- [coverage: high -- direct file paths, all verified present] -->

- [[backend-attendance]] — the whole backend path: the `Occurrence` aggregate, `OccurrenceWriter`,
  `MarkAttendanceApplicationService`, `SyncAttendanceApplicationService`, `AttendanceRestController`.
- [[backend-identity]] — `CallerResolver` resolves the Keycloak subject to a User; `persons` and the
  walk-in Directory search are identity's.
- [[backend-common-domain]] — `CallerResolver`, `UserActivityRecorder`, `OptimisticLockException`.
- [[backend-container]] — `slice-2` (core schema + occurrence version), `slice-4` (`client_marked_at`),
  `slice-7` (walk-in marking type) migrations; the `ProblemDetail` mapping that turns
  `StaleRosterException` into `ROSTER_STALE`.
- Mobile: `sabha_attendance/lib/roster/`, `lib/sync/` (`attendance_store.dart`, `sync_engine.dart`,
  `pending_marking.dart`), `lib/walk_in/`.

## Amendments

<!-- [coverage: medium -- reconstructed from the changelog directory names and ADR consequences; the issue-to-change mapping is inferred, not read from the PRs] -->

- **Slice 2** — the original roster + marking path and the core schema.
- **Slice 4** (issue #5) — offline sync: `client_marked_at` on `attendance_markings`, the local
  sqflite queue, `POST /api/sync`, and the 7-day roster cap.
- **Slice 7** — Walk-in marking, adding `MarkingType` and routing Walk-ins through the same
  load/retry/save path as Roster markings.
- **Issue #128** — `OccurrenceWriter` extracted so one class owns retry, audit and publish for
  every writer of the aggregate; marking moved to its unaudited path.

**Divergence from ADR-0007, worth knowing:** the ADR only rules out marking a Walk-in who is *not
yet in the Directory* offline. The implementation is stricter — recording a Walk-in is online-only
outright, stated in `walk_in_api.dart`. Nothing has amended the ADR to match.

## Sources

- [ADR-0001](../../adr/0001-sabha-occurrence-lifecycle.md), [ADR-0003](../../adr/0003-platform-split-by-role.md), [ADR-0007](../../adr/0007-offline-capable-attendance-marking.md), [ADR-0013](../../adr/0013-directory-de-duplication-on-person-add.md)
- [CONTEXT.md](../../../CONTEXT.md) — Attendance Marking, Walk-in, Roster, Sabha Occurrence, Sanchalak, Sah-Sanchalak
- `Occurrence.java`, `MarkAttendanceApplicationService.java`, `SyncAttendanceApplicationService.java`
- `apps/mobile/sabha_attendance/lib/sync/sync_engine.dart`, `apps/mobile/sabha_attendance/lib/walk_in/walk_in_api.dart`
