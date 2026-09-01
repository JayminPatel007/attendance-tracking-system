---
type: feature
title: Re-engagement
description: Surfacing the People who have stopped attending their Home Sabha, so the responsible Karyakar can follow up.
aliases: [re-engagement candidate, drifting, missed streak, consecutive missed Occurrences, priority candidate, tier, thresholds, candidate projection, follow-up list]
source_paths: [
  apps/backend/analytics-service/*/src/main/**,
  apps/backend/application-container/src/main/java/**/*CronJobs.java,
  apps/web/src/app/sections/dashboard/threshold-editor.component.ts,
  apps/backend/application-container/src/main/resources/db/changelog/slice-15/**,
  docs/adr/0008-*.md,
  docs/adr/0010-*.md,
  docs/adr/0018-*.md,
  CONTEXT.md
]
issues: [16]
sources:
  - { id: adr-0008, title: "Single Bounded Context, with Internal Package Seams", resource: ../../adr/0008-single-bounded-context-with-internal-seams.md }
  - { id: adr-0010, title: "Re-engagement Candidate Definition", resource: ../../adr/0010-re-engagement-candidate-definition.md }
  - { id: adr-0018, title: "Application services split: `*-application` vs `*-application-service`", resource: ../../adr/0018-application-service-split.md }
  - { id: context, title: "CONTEXT.md — Re-engagement candidate, Home Sabha, Walk-in, Madhyastha Karyalaya", resource: ../../../CONTEXT.md }
last_compiled: 86a4e5242ce1f547f13bb0411745db918726a921
---

# Re-engagement

## What it does

<!-- [coverage: high -- ADR-0010 read against HomeSabhaHistory, Thresholds and the projection scanner] -->

A Person who stops coming to their **Home Sabha** becomes a **re-engagement candidate**, and the
system surfaces them by name so the Karyakar responsible can call them.

The measure is **consecutive missed Home Sabha Occurrences**, not a percentage and not a calendar
window: three in a row is decision-actionable, six is escalation-actionable, and both numbers are
Madhyastha Karyalaya configuration rather than constants. A **Walk-in elsewhere does not reset the
streak** — attending the wider organisation is not attending the Sabha you are on the roll of, and
ADR-0010 says the corrective for that is a [home-sabha-transfer](home-sabha-transfer.md), not a silent reset.

The streak is per **(Person, Home Sabha)**, so someone can be drifting from their Yuvak Sabha and
present at their Sanyukta one. The list itself is read through [dashboards](dashboards.md).

## Flow

<!-- [coverage: high -- the calculator, the scanner, both adapters and the cron entry point read end to end] -->

**Background** — nothing computes this on demand.

1. `AnalyticsCronJobs` fires every 15 minutes and calls `ReEngagementProjectionScanner.refresh()`.
2. The scanner runs `ReEngagementCandidateCalculator` State-wide and replaces the whole projection.
3. `JdbcHomeSabhaOccurrenceHistory` supplies the raw material: per (Person, Home Sabha), the
   concluded Occurrences of that Sabha classified `PRESENT` / `ABSENT` / `CANCELLED`, UNIONed with
   the Person's Walk-ins elsewhere, in date order.
4. `HomeSabhaHistory.currentMissedStreak()` walks that stream backwards; `Thresholds.classify()` turns
   the streak into a tier or into nothing.
5. `JdbcCandidateProjectionStore.replaceAll` clears and re-inserts `reengagement_candidates`.

**Web** — the Madhyastha Karyalaya's threshold editor, on the dashboard's overview tab. It is
self-gating: it renders nothing and reads nothing unless the session says MK, and it re-checks the
invariant locally before `PUT /bff/dashboard/thresholds` checks it again.

**Mobile** — `_none_`.

## Rules & authority

<!-- [coverage: high -- ADR-0010's consequences cross-checked against OutcomeKind, Thresholds and the history SQL] -->

- **What counts.** `ABSENT` extends the streak, `PRESENT` ends it, and `CANCELLED` and
  `WALK_IN_ELSEWHERE` are stepped over — they neither extend nor reset. A Cancelled Occurrence never
  expected attendance, so counting it would manufacture candidates out of a festival break.
- **Only concluded Occurrences are facts.** The history reads `FINALIZED` and `CANCELLED` states only;
  an Occurrence still open for marking has no outcome yet.
- **A Person does not inherit a streak.** Outcomes count only from the Person's membership date at
  that Home Sabha onward, so a fresh transfer starts clean.
- **The thresholds are owned, not hardcoded.** A single-row `analytics_thresholds` table, seeded 3/6,
  readable by any resolved caller and writable only by the Madhyastha Karyalaya — **403** otherwise.
  The invariant `priority >= candidate >= 1` lives on the `Thresholds` record and refuses a bad pair
  with **422**, so the check cannot be skipped by writing the table through another door.
- **Who sees which candidates is not decided here** — the calculator runs State-wide and the scoping
  happens at read time. See [dashboards](dashboards.md) and [authorization](../patterns/authorization.md).
- **The rebuild is atomic and wholesale.** `refresh()` carries the transaction so a concurrent read
  sees the old projection or the new one, never a half-rebuilt one; a missed tick self-heals.

## Where the code is

<!-- [coverage: high -- direct paths, all verified present] -->

- [backend-analytics](../structure/backend-analytics.md) — `ReEngagementCandidateCalculator`, `ReEngagementProjectionScanner`,
  `HomeSabhaHistory`, `Thresholds`, `Tier`, `OutcomeKind`, `Scope`, and the four adapters behind them.
- [backend-container](../structure/backend-container.md) — `AnalyticsCronJobs` and the cron expression; `slice-15`'s
  `analytics_thresholds` and `reengagement_candidates`.
- [web](../structure/web.md) — the threshold editor inside the `dashboard` section.
- The tables the history reads — `home_sabhas`, `occurrences`, `attendance_markings` — belong to
  [backend-identity](../structure/backend-identity.md) and [backend-attendance](../structure/backend-attendance.md); analytics only queries them.

## Amendments

<!-- [coverage: medium -- reconstructed from the slice-15 changelog headers and class javadocs; the issue-to-change mapping is inferred] -->

- **Slice 15** (issue #16) — the whole capability: the calculator, the two tables, the scheduled
  projection and the MK threshold editor.

**Worth knowing:** `Scope.OfSabhas` exists but nothing in production passes it. The scanner always
runs `Everything`; the narrower case is the seam for an incremental refresh and what keeps the
calculator's tests database-free. Treat it as designed-for, not used.

**Divergence from ADR-0010, worth knowing:** the ADR asks the analytic to *"show recent transitions
so follow-up calls aren't repeated"*. The projection stores only the current streak and tier, so
candidate → off-list → candidate again is invisible. Nothing has amended the ADR to match.

## Method

- `HomeSabhaHistory.currentMissedStreak()` is the source that paid, and it is eleven lines: the
  backwards walk with two outcomes counted and two stepped over is the whole ADR-0010 rule in
  executable form, and it settles the Cancelled/Walk-in questions the ADR states in prose.
- `JdbcHomeSabhaOccurrenceHistory`'s SQL carries the two facts written down nowhere else — the
  membership-window join, and that only concluded Occurrences are outcomes.
