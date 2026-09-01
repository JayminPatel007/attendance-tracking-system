---
type: feature
title: Selection
description: Nominating a Person into a selective Bal Sevak or Yuvak Sevak Sabha, and deselecting them again.
aliases: [BSS, YSS, Bal Sevak Sabha, Yuvak Sevak Sabha, selective track, nominate, nomination, approve, reject, deselect, selective Home Sabha]
tags: [bff]
source_paths: [
  apps/backend/identity-service/*/src/main/**,
  apps/mobile/sabha_attendance/lib/selection/**,
  apps/web/src/app/sections/selection/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-16/**,
  docs/adr/0003-*.md,
  docs/adr/0006-*.md,
  docs/adr/0022-*.md,
  CONTEXT.md
]
issues: [17, 73]
sources:
  - { id: adr-0003, title: "Platform Split: Mobile for Sabha-Level Operations, Web for Everything Else", resource: ../../adr/0003-platform-split-by-role.md }
  - { id: adr-0006, title: "BSS Membership Is Additive, Not Replacement", resource: ../../adr/0006-bss-is-additive-not-replacement.md }
  - { id: adr-0022, title: "Web session via a Backend-for-Frontend with an HTTP-only cookie", resource: ../../adr/0022-web-session-via-bff-http-only-cookie.md }
  - { id: context, title: "CONTEXT.md — Selection (BSS / YSS), Bal Sevak Sabha, Yuvak Sevak Sabha, Home Sabha, Nirdeshak, Sanchalak", resource: ../../../CONTEXT.md }
last_compiled: 86a4e5242ce1f547f13bb0411745db918726a921
---

# Selection

## What it does

<!-- [coverage: high -- CONTEXT.md's Selection entry and ADR-0006, read against SelectionNomination and SelectionService] -->

**Bal Sevak Sabha** and **Yuvak Sevak Sabha** are selective programs, and a Person joins one in two
steps: their **Regular Sanchalak nominates**, and the **demographic Nirdeshak** approves or rejects.

Approval is **additive**. The Person gains the selective Sabha as an *additional* Home Sabha and
keeps their Regular one, which is the whole point of ADR-0006: if selection moved children out of
Regular, every Regular roll-up would systematically exclude the most committed of its cohort.
Deselection removes that one Home Sabha and nothing else.

**Criteria are not system-enforced.** Nothing checks age, attendance or readiness — the two Karyakars
apply their own judgement, and the system facilitates and records the exchange. What it enforces is
who may act, that the Person is on the nominating roster, and that nobody is nominated twice.

## Flow

<!-- [coverage: medium -- backend path read end to end; the two client halves read from their api/controller files, not their widgets or templates] -->

**Mobile** — nomination only, on a dedicated screen. `POST /api/sanchalak/nominations` carries the
Person and the Regular Sabha, and nothing else: the selective Sabha is derived server-side, so the
Sanchalak only chooses a Person. Online-only — a nomination is never queued.

**Web** — decision only, in the `selection` section: the pending queue (approve, or reject with a
comment) and the currently-selected People (deselect). Both are scoped server-side to the caller's
Nirdeshak rows, so the screen sends no scope.

This split is ADR-0003 exactly — the Sanchalak works from the phone, the Nirdeshak from the web.

**Backend** — `SelectionService` owns the lifecycle. On nominate it checks the caller's roles on the
Sabha and the Person's roster membership, derives the track from the demographic (`BAAL`/`BALIKA` →
BSS, `YUVAK`/`YUVATI` → YSS) and the selective Sabha from `(Kshetra, demographic, track)`, refuses a
Person already selected or already pending, and saves a `PENDING` nomination. Approve, reject and
deselect each load the nomination, check the demographic Nirdeshak, move the aggregate and — on
approve and deselect only — add or remove the selective Home Sabha.

## Rules & authority

<!-- [coverage: high -- SelectionService and SelectionNomination read directly against ADR-0006 and CONTEXT.md] -->

- **Nominating is the Sabha's Sanchalak or Sah-Sanchalak**, on that Sabha; anyone else is **403**.
- **Deciding is the demographic Nirdeshak**, and that authority is **track-shared** — the same
  Nirdeshak owns Regular and selective for the demographic, because deciding who is selected is
  itself an act of leadership over the whole cohort. There is no Nirikshak tier for the selective
  track at all.
- **Selection has no Authorization Engine.** Both checks are single port questions — a role set on a
  Sabha, and `holdsNirdeshak(user, kshetra, demographic)` — reusing the appointment ladder's authority
  lookup rather than adding an engine to answer a one-line question. See [authorization](../patterns/authorization.md).
- **Deselection is a direct action, not an undo.** The Nirdeshak names a Person and a selective Sabha;
  the approved nomination is found from that pair and moved to `DESELECTED`. It does not require the
  original nomination id, and only an `APPROVED` one can be deselected.
- **The nomination row is the audit trail.** Who nominated, who decided, when, and any rejection
  reason live on it — which is why it is a source table for [audit-log](audit-log.md) and needs no separate record.
- **Rejections.** Already selected, or a second open nomination on the same track → **409** (a partial
  unique index enforces the second one in the database as well). Not on the roster, no selective
  track for the demographic, an already-decided nomination → **422**. No selective Sabha in the
  Kshetra, or an unknown nomination → **404**.

## Where the code is

<!-- [coverage: high -- direct paths, all verified present] -->

- [backend-identity](../structure/backend-identity.md) — the `selection` feature package: `SelectionService`, the
  `SelectionNomination` aggregate and its four events, `SelectionRoster`, `SelectionQueries`,
  `SelectionRepository`, and the BFF and mobile controllers.
- [backend-sabha](../structure/backend-sabha.md) — `JdbcStructuralHierarchyLookup`, which answers both derivations: the
  Sabha's `(Kshetra, demographic)` scope and the selective Sabha in it.
- [backend-common-domain](../structure/backend-common-domain.md) — `RoleAssignmentLookup`, `StructuralHierarchyLookup`, `CallerResolver`,
  `DomainEventPublisher`, `SabhaKind`.
- [backend-container](../structure/backend-container.md) — `slice-16`'s `selection_nominations`, its queue index and its
  one-pending partial unique index, plus the selective-Sabha and Nirdeshak seed.
- [mobile-shell](../structure/mobile-shell.md) — `lib/selection/`; [mobile-sabha-api](../structure/mobile-sabha-api.md) — the generated
  `SelectionRestControllerApi` it calls through.
- [web](../structure/web.md) — the `selection` section.

## Amendments

<!-- [coverage: medium -- reconstructed from the slice-16 changelog header and class javadocs; the issue-to-change mapping is inferred] -->

- **Slice 16** (issue #17) — the whole capability: the aggregate, the table, the mobile nominate
  screen and the web queue.
- **Issue #73** — both clients moved onto generated typed clients; the mobile wrapper bridges the
  generated exception back onto the shared error seam.

**Worth knowing:** the Sanyukta kind has no selective counterpart, so `SelectiveTrack.forDemographic`
throws for it. A Sanyukta Sanchalak's nominate attempt fails at the derivation with a 422 rather than
being refused up front — the screen never offers it, so the path is unreachable in practice.

## Method

- `SelectionService.nominate` is the source that paid: its five guards in order are the entire
  contract, and the two derivations inside it — demographic → track, then `(Kshetra, demographic,
  track)` → Sabha — are stated in no ADR. `CONTEXT.md` describes the outcome and never says who
  derives the target Sabha.
- The `slice-16` migration header supplied the two things the Java does not: why the row is
  denormalized (the queue scopes without re-walking the hierarchy) and that the row doubles as the
  audit source.
