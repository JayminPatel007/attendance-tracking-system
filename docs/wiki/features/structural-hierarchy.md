---
type: feature
title: Structural Hierarchy
description: Creating and deleting the City, Zone and Kshetra chain the whole organisation hangs off.
aliases: [structural admin, geography, City, Zone, Kshetra, Sanyojak, Regional Team, Madhyastha Karyalaya, block-if-non-empty, tier above]
tags: [bff]
source_paths: [
  apps/backend/sabha-service/*/src/main/**,
  apps/web/src/app/sections/structural-admin/**,
  apps/web/projects/sabha-domain/src/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-10/**,
  docs/adr/0003-*.md,
  docs/adr/0009-*.md,
  docs/adr/0024-*.md,
  docs/adr/0026-*.md,
  CONTEXT.md
]
issues: [11, 84, 88]
sources:
  - { id: adr-0003, title: "Platform Split: Mobile for Sabha-Level Operations, Web for Everything Else", resource: ../../adr/0003-platform-split-by-role.md }
  - { id: adr-0009, title: "Structural Creation Authority Lives at the Tier Above", resource: ../../adr/0009-structural-creation-authority.md }
  - { id: adr-0024, title: "Zone creation moves from Madhyastha Karyalaya to the Regional Team", resource: ../../adr/0024-zone-creation-moves-to-regional-team.md }
  - { id: adr-0026, title: "Deletion model: block-if-non-empty for geography, soft-retire for Sabha Kind, revoke-with-inheritance for roles", resource: ../../adr/0026-deletion-model.md }
  - { id: context, title: "CONTEXT.md — Geographic hierarchy, Kshetra, Sanyojak, Regional Team, Madhyastha Karyalaya", resource: ../../../CONTEXT.md }
last_compiled: 85eaa7a00240b54e15e35da00229a19ee8c71ce7
---

# Structural Hierarchy

## What it does

<!-- [coverage: high -- ADR-0009/0024/0026 read against StructuralCreationService and StructuralDeletionService] -->

The organisation is a chain — **State → City → Zone → Kshetra → Sabha** — and this capability is how
the top four links of it come into existence and go away again. A Madhyastha Karyalaya member
registers a **City**; a **Regional Team** member of that City creates its **Zones**; the Zone's
**Sanyojak** creates its **Kshetras**. Every node carries a `createdBy` naming the User who made it,
so structure is self-serve rather than an ops request (ADR-0009).

Deletion is the same authority read backwards, and it is **block-if-non-empty**: a node may be
removed only while nothing lives under it. Nothing cascades and nothing soft-deletes, because
attendance history must survive (ADR-0026).

The fifth link — defining a Sabha, deleting one, and registering the Kind it is an instance of —
is [sabha-definition](sabha-definition.md), which holds the whole Sabha rather than splitting it across two pages.

## Flow

<!-- [coverage: medium -- backend read end to end; the web half read from the component and its shared helpers, not from the template] -->

**Web** — the only surface. Per ADR-0003 structure is a web-tier job, and `structural-admin` renders
one tab set per actor: `cities` for the MK, `zones` for a Regional Team member, `kshetras` for a
Sanyojak. The MK's second tab, the Kind registry, is [sabha-definition](sabha-definition.md)'s. The tab set is
chosen from the session's own authority flags, so the client never invents a permission it was not
granted.

1. `GET /bff/structure/cities|zones|kshetras` populate the lists. Each view carries its **live child
   count**, which is the whole reason the delete button can be disabled before anyone clicks it.
2. `notEmptyReason(count, noun)` in the shared `sabha-domain` library turns that count into the
   blocking phrase, kept byte-identical to the server's, and `DeleteButtonComponent` renders either
   a disabled button carrying the reason or a live Delete.
3. `POST /bff/structure/{cities|zones|kshetras}` creates; `DELETE` on the same paths removes.
   `my-zones` and `my-cities` narrow the pickers to what the caller actually holds.

**Backend** — `sabha-application` → `sabha-application-service` → the aggregates:

- `StructuralCreationController` and `StructuralDeletionController` resolve the Keycloak subject to a
  User, then hand off; both are `/bff/*` only.
- `StructuralScopeAuthority` answers *"does this caller hold the tier above?"* for **both** create and
  delete, so a tier's authority cannot drift between the two paths.
- `StructuralCreationService` authorizes, then lets `City`/`Zone`/`Kshetra` enforce their own
  invariants and stamp `createdBy`.
- `StructuralDeletionService` authorizes, counts children, and deletes only on zero.

**Mobile** — `_none_`.

## Rules & authority

<!-- [coverage: high -- StructuralScopeAuthority and StructuralDeletionService read directly, cross-checked against ADR-0009/0024/0026] -->

- **Who creates what.** City ← Madhyastha Karyalaya (state scope). Zone ← any Regional Team member of
  that City. Kshetra ← that Zone's Sanyojak. The four predicates are four cross-context lookups —
  [authorization](../patterns/authorization.md).
- **The MK has no Zone path at all.** ADR-0024 moved Zone creation down and removed the fallback
  deliberately, so one entity never has two possible creators. A new City therefore has no Zones
  until its first Regional Team member exists — a deliberate bootstrap window.
- **Delete is authorized by current scope, not by `created_by`.** A replacement Sanyojak may delete a
  Kshetra their predecessor created.
- **Rejections.** Denial → **403**. A non-empty node → **409** `StructuralNotEmptyException`, carrying
  the human-readable reason (*"has 6 Kshetras"*). An unknown id → **404**. Order matters: City checks
  existence before authority, the other three resolve the parent first.
- **Nothing cascades.** To remove a Zone you dismantle its Kshetras, then its Sabhas, first.
- **The web enforces nothing.** The tabs and the disabled buttons are navigation; every refusal above
  is the server's.

## Where the code is

<!-- [coverage: high -- direct paths, all verified present] -->

- [backend-sabha](../structure/backend-sabha.md) — the whole backend path: the three controllers, `StructuralScopeAuthority`,
  `StructuralCreationService`, `StructuralDeletionService`, the `City`/`Zone`/`Kshetra` aggregates and
  the `Jdbc*` repositories behind them.
- [backend-identity](../structure/backend-identity.md) — implements every authority lookup this capability asks: the MK, Regional
  Team, Sanyojak and role-assignment adapters.
- [backend-common-domain](../structure/backend-common-domain.md) — those ports, `AuthorizedAction`, `AuthorizationDeniedException`, `CallerResolver`.
- [backend-container](../structure/backend-container.md) — `slice-10`'s `cities` and `zones` tables and the `kshetras` back-fill columns; the `ProblemDetail` mapping the 409 travels in.
- [web](../structure/web.md) — the `structural-admin` section and the `sabha-domain` library's `notEmptyReason` and
  `DeleteButtonComponent`.

## Amendments

<!-- [coverage: medium -- reconstructed from changelog headers, class javadocs and ADR supersession notes; the issue-to-change mapping is inferred] -->

- **Slice 10** (issue #11) — the capability: the geographic chain, `created_by` on every tier, and
  `role_assignments.zone_id` so a Sanyojak's scope is representable.
- **ADR-0024** (issue #84) — Zone creation moved MK → Regional Team, adding `RegionalTeamCityLookup`,
  `my-cities`, and the `regionalTeam` session flag.
- **ADR-0026** (issue #88) — deletion: `StructuralDeletionService`, the block-if-non-empty counts, and
  the shared delete control. **No migration** — block-if-non-empty needs no column.

## Method

- `StructuralScopeAuthority` is the source that paid: four one-line predicates and a class javadoc
  that states the create/delete symmetry outright. ADR-0009's table is superseded in one row by
  ADR-0024, and only this file shows which reading won.
- The client-side `notEmptyReason` was worth reading beside `StructuralNotEmptyException`: the
  byte-identical wording is a deliberate contract, invisible from either file alone.
