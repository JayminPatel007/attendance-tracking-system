---
type: feature
title: Occurrence Lifecycle
description: How a Sabha Occurrence comes into being, opens, finalizes, and is shaped or reopened along the way.
aliases: [Sabha Occurrence, occurrence states, auto-open, auto-finalize, materialization, cancel, revert, reschedule, venue override, reopen, Effective Slot, monthly ad-hoc]
tags: [audit, bff]
source_paths: [
  apps/backend/attendance-service/*/src/main/**,
  apps/backend/application-container/src/main/java/**/*CronJobs.java,
  apps/mobile/sabha_attendance/lib/occurrence_control/**,
  apps/mobile/sabha_attendance/lib/monthly_occurrence/**,
  apps/web/src/app/sections/occurrence-reopen/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-3/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-5/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-12/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-13/**,
  docs/adr/0001-*.md,
  docs/adr/0012-*.md,
  docs/adr/0021-*.md,
  CONTEXT.md
]
issues: [14, 128]
sources:
  - { id: adr-0001, title: "Sabha Occurrence Lifecycle", resource: ../../adr/0001-sabha-occurrence-lifecycle.md }
  - { id: adr-0012, title: "Sabha Schedule Shapes and Occurrence Materialization", resource: ../../adr/0012-sabha-schedule-shapes-and-occurrence-materialization.md }
  - { id: adr-0021, title: "Spring Scheduling for Occurrence cron jobs", resource: ../../adr/0021-spring-scheduling-for-occurrence-cron.md }
  - { id: context, title: "CONTEXT.md — Sabha Occurrence, Occurrence States, Effective Slot, Sanchalak, Nirikshak", resource: ../../../CONTEXT.md }
last_compiled: 85eaa7a00240b54e15e35da00229a19ee8c71ce7
---

# Occurrence Lifecycle

## What it does

<!-- [coverage: high -- ADR-0001's state machine read against Occurrence.java, the three scanners and OccurrenceReopenService] -->

A **Sabha Occurrence** is one gathering of a Sabha, and it moves through an explicit state machine:
`Scheduled ↔ (Rescheduled | Cancelled) → Open for Marking → Finalized`, with a higher-tier **reopen**
as the only way back out of `Finalized`.

Occurrences arrive two ways, decided by the Sabha's schedule shape (ADR-0012). A **weekly** Sabha has
its Occurrences materialized automatically on a rolling eight-week window; a **monthly ad-hoc** Sabha
has none until its Sanchalak creates this month's by hand, on a date they pick.

Along the way the Sanchalak may **shape** the Occurrence — cancel it, revert that cancellation,
reschedule it, or set a one-off venue — none of which touches the Sabha's standing schedule.

Marking attendance inside the open window is [attendance-marking](attendance-marking.md); a Nirikshak doing any of the
above for an absent Sanchalak is [sanchalak-proxy](sanchalak-proxy.md).

## Flow

<!-- [coverage: medium -- backend and cron read end to end; the mobile and web halves read from their api/controller files, not their widgets] -->

**No actor at all, for two of the transitions.** `OccurrenceCronJobs` in the container holds three
`@Scheduled` entry points (ADR-0021) over scanners that live in attendance: auto-open every minute,
auto-finalize hourly, weekly materialization nightly. Each scanner re-derives its work from current
state, so a missed firing self-heals on the next tick.

**Mobile** — the Sanchalak's surface. `occurrence_control` reads
`GET /api/sanchalak/current-occurrence` and posts `cancel` / `revert` / `reschedule` /
`venue-override`; `monthly_occurrence` lists the monthly Sabhas with their compliance nudge and posts
`POST /api/sabhas/{id}/occurrences`. Both are **online-only** — never queued in the offline engine.

**Web** — the reopen surface only. `occurrence-reopen` lists the Occurrences the caller may reopen
(server-scoped) and posts `POST /bff/occurrences/{id}/reopen` with a required reason.

**Backend** — every one of those paths funnels into one class:

- `OccurrenceShapingService`, `OccurrenceReopenService` and the two scanners each own only their own
  vocabulary and preconditions.
- `OccurrenceWriter` owns everything else: load, mutate, save, retry the whole cycle up to three
  times on an optimistic-lock conflict, append the audit row, publish the events. Nothing is written
  until the save sticks, and the audit row is stamped only then.
- `EffectiveSlotResolver` turns an Occurrence into absolute start and end instants, per-boundary:
  an override wins, anything not overridden falls back to the standing schedule.

## Rules & authority

<!-- [coverage: high -- AuthorizationEngine, OccurrenceWriter and Occurrence read directly against ADR-0001] -->

- **A sealed actor.** `TransitionActor` is either `Cron` or `SignedIn`. Cron holds no role, bypasses
  the engine entirely, and is audited as `SYSTEM`; a signed-in caller is resolved and authorized
  against the Occurrence's Sabha before the mutation runs — [authorization](../patterns/authorization.md).
- **Shaping is Sanchalak-only.** Sah-Sanchalak is explicitly excluded: their authority is the day-of
  toolkit, not the decision whether the Sabha runs. The assigned Nirikshak may act as proxy.
- **Reopen is the Kshetra tier only** — Nirikshak, Nirdeshak, Sah-Nirdeshak — and never the Sanchalak
  who owns shaping, nor the oversight tiers (Sanyojak, Sant, MK), who are kept out of the data-edit
  path by design.
- **Two windows, both 24h and both configurable.** Auto-finalize fires a grace period after the
  Effective Slot ends; the revert window closes the same interval after it. Past that, a Cancelled
  Occurrence locks.
- **Reasons.** Cancel and reopen both require one, refused otherwise. Both land on the audit row.
- **The "reopened" badge is derived**, from `occurrence_state_transitions` — there is no denormalized
  column, so the badge cannot disagree with the log.
- **Rejections.** Denial → **403**. An unknown Occurrence → **404**. An illegal transition, an expired
  revert window, or a manual create against a weekly Sabha → domain refusal. Three lost optimistic
  locks → `ConcurrentModificationException`.

## Where the code is

<!-- [coverage: high -- direct paths, all verified present] -->

- [backend-attendance](../structure/backend-attendance.md) — the `Occurrence` aggregate, `OccurrenceWriter`, `TransitionActor`,
  `EffectiveSlotResolver`, the shaping / reopen / monthly-create services, the three scanners, and the
  reopen projection.
- [backend-container](../structure/backend-container.md) — `OccurrenceCronJobs`, the `Clock` bean the scanners take, and `slice-3`
  (`occurrence_state_transitions`), `slice-5` (the shaping columns), `slice-13`.
- [backend-sabha](../structure/backend-sabha.md) — the schedule, shape and weekly-catalog lookups the scanners resolve slots
  through.
- [backend-common-domain](../structure/backend-common-domain.md) — `AggregateRoot`, `DomainEventPublisher`, `OptimisticLockException`,
  `CallerResolver`, `AuthorizedAction`.
- [mobile-shell](../structure/mobile-shell.md) — `lib/occurrence_control/` and `lib/monthly_occurrence/`.
- [web](../structure/web.md) — the `occurrence-reopen` section.

## Amendments

<!-- [coverage: medium -- reconstructed from changelog headers and class javadocs; the issue-to-change mapping is inferred] -->

- **Slice 3** — the cron scanners and the state-transition audit table.
- **Slice 5** — Sanchalak shaping: the four override columns and the four actions.
- **Slice 12** (issue #13) — weekly materialization and manual monthly creation, both keyed off the
  schedule shape.
- **Slice 13** (issue #14) — the higher-tier reopen, its Kshetra-scoped authority and its badge.
- **Issue #128** — `OccurrenceWriter` extracted, the actor sealed, and `@Transactional` moved onto the
  write methods so the cron thread, which has no ambient transaction, still gets one per Occurrence.

## Method

- `OccurrenceWriter`'s javadoc is the source that paid, and it is the only place the cron path is
  legible: it states why the write methods are transactional (the scheduled thread has no ambient
  transaction) and why the audit row is stamped after the save. A page compiled from the state
  machine alone would have described half the capability.
- `Occurrence.java`'s per-method javadocs carry the split of what the aggregate guards versus what the
  application service does — the grace windows and the reason requirements are deliberately *not* in
  the aggregate, and only these comments say so.
