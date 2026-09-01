---
type: structure
title: Sabha Service
description: Owns the structural hierarchy — Kshetra, Zone, City — and the Sabha definitions and Sabha Kinds hung off it.
resource: apps/backend/sabha-service
aliases: [the structural hierarchy, Kshetra, Zone, City, Sabha Kind]
tags: [bff]
source_paths: [
  apps/backend/sabha-service/*/src/main/**,
  apps/backend/sabha-service/*/pom.xml,
  apps/backend/sabha-service/pom.xml,
  docs/adr/0009-*.md,
  docs/adr/0012-*.md,
  docs/adr/0019-*.md,
  docs/adr/0024-*.md,
  docs/adr/0026-*.md,
  CONTEXT.md
]
sources:
  - { id: adr-0009, title: "Structural Creation Authority Lives at the Tier Above", resource: ../../adr/0009-structural-creation-authority.md }
  - { id: adr-0012, title: "Sabha Schedule Shapes and Occurrence Materialization", resource: ../../adr/0012-sabha-schedule-shapes-and-occurrence-materialization.md }
  - { id: adr-0019, title: "Bounded-context module taxonomy: five modules per context, presentation split from application service", resource: ../../adr/0019-bounded-context-module-taxonomy.md }
  - { id: adr-0024, title: "Zone creation moves from Madhyastha Karyalaya to the Regional Team", resource: ../../adr/0024-zone-creation-moves-to-regional-team.md }
  - { id: adr-0026, title: "Deletion model: block-if-non-empty for geography, soft-retire for Sabha Kind, revoke-with-inheritance for roles", resource: ../../adr/0026-deletion-model.md }
  - { id: context, title: "CONTEXT.md — Kshetra, Zone, Sabha, Sabha Kind, Sanyojak, Regional Team", resource: ../../../CONTEXT.md }
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---

# Sabha Service

## Purpose

<!-- [coverage: high -- class listing + ADR-0009/0024/0026] -->

Owns the **structural hierarchy** — City, Zone, Kshetra — and the **Sabha** itself, including
Sabha Kind and its schedule shape. It answers "where in the organisation does this Sabha sit?" for
everyone else, and it is the authority on creating and deleting structure (ADR-0009, ADR-0024,
ADR-0026).

It does **not** own who attends a Sabha or who runs it: rosters and role assignments are
[backend-identity](backend-identity.md)'s, occurrences are [backend-attendance](backend-attendance.md)'s.

## Layout

<!-- [coverage: medium -- directory + class listing only; every package-info.java here is an empty ADR-0019 scaffold and carries no information] -->

The standard five-module [module-ring](../patterns/module-ring.md). Small unit — 50 main source
files, roughly a quarter of identity's.

| Module | Main files | Holds |
|---|---|---|
| `sabha-domain/sabha-domain-core` | 22 | `City`, `Zone`, `Kshetra`, `Sabha`, `SabhaKind`, plus `Demographic`, `Track`, `ScheduleShape`, `StructuralNames` and the not-found / not-empty exceptions. |
| `sabha-domain/sabha-application-service` | 11 | `StructuralCreationService`, `StructuralDeletionService`, `SabhaKindLifecycleService`, `StructuralScopeAuthority`, `StructuralQueries`, and the five repository ports. |
| `sabha-data-access` | 12 | 11 `Jdbc*` adapters, five of which implement common-domain ports for other contexts. |
| `sabha-application` | 4 | 3 REST controllers (ADR-0017). |
| `sabha-messaging` | 1 | `package-info.java` only — an empty scaffold; this unit sends nothing. |

No feature-package line: at 11 application-service classes in one flat package, the ring *is* the
navigation axis here. That line only earns its place on a unit the size of identity.

## Exposes

<!-- [coverage: high -- mapping-annotation grep over sabha-application] -->

3 controllers, **all `/bff/*`** — this unit has no mobile-facing surface at all, which follows from
ADR-0003: structural creation is a web-tier job.

| Prefix | Serves | Controllers |
|---|---|---|
| `/bff/structure/*` | web | `StructuralCreationController` (12 routes), `StructuralDeletionController` (3 routes) |
| `/bff/sabhas/mine`, `DELETE /bff/sabhas/{id}` | web | `SabhaListController`, `StructuralDeletionController` |

`POST /bff/sabhas` is **not** here — Sabha definition is served by [backend-identity](backend-identity.md), because the
authority check is identity's under ADR-0029 even though the Sabha it creates is this unit's — see
[authorization](../patterns/authorization.md).

## Talks To

<!-- [coverage: high -- import scan of all four modules] -->

Zero direct imports of another context's packages; everything goes through `org.sabha.common`.

**Outbound** — ports sabha consumes, all implemented in `identity-data-access`:

| Port | Target | Used by |
|---|---|---|
| `RoleAssignmentLookup` | identity | `StructuralScopeAuthority` |
| `SanyojakZoneLookup` | identity | `StructuralScopeAuthority` |
| `NirdeshakScopeLookup` | identity | `StructuralScopeAuthority`, `SabhaListController` |
| `RegionalTeamCityLookup` | identity | `StructuralScopeAuthority` (Zone creation, ADR-0024) |
| `MadhyasthaKaryalayaLookup` | identity | `StructuralScopeAuthority` |
| `CallerResolver` | identity | all three controllers |

**Inbound** — five common-domain ports sabha implements for the other three contexts:
`StructuralHierarchyLookup` (the busiest edge in the backend — identity, attendance and analytics
all use it), `SabhaProvisioning`, `SabhaScheduleLookup`, `SabhaShapeLookup`, `WeeklySabhaCatalog`.

## Data

<!-- [coverage: medium -- writer grep across all four contexts; ownership inferred from writers, no ownership manifest exists] -->

Migrations are central (see `protocol.md` §8) and outside this page's `source_paths`; the table-name
grep is what reaches them.

**Owned** (sabha is the only writer): `cities`, `zones`, `kshetras`, `sabha_kinds`, `sabhas`.

**Read but not owned**: `occurrences` (attendance) — counted by `JdbcSabhaRepository` and
`JdbcStructuralQueries` for the block-if-non-empty check and the admin listing.

Deletion is **block-if-non-empty** across all four levels (ADR-0026): a City with Zones, a Zone with
Kshetras, a Kshetra with Sabhas or a Sabha with Occurrences all reject with
`StructuralNotEmptyException`. There is no cascade and no soft-delete column. Sabha Kind is the one
exception — it soft-retires via `retired_at`/`retired_by` rather than deleting.

## Gotchas

<!-- [coverage: medium -- two verified from class names and the common-domain listing; not an exhaustive sweep] -->

- **`SabhaKind` is two different types.** `org.sabha.sabha.domain.SabhaKind` is the aggregate this
  unit writes; `org.sabha.common.SabhaKind` is the cross-context value other contexts read. Same
  simple name, so an IDE auto-import lands on the wrong one silently.
- Every `package-info.java` in this unit reads "Empty scaffold per ADR-0019" and describes only the
  Clean Architecture ring. Unlike identity's, they carry **no** information about what actually
  lives in the module — don't budget for them as a source here.

## Covered by

<!-- [coverage: high -- derived: the three dossiers below name this page; the candidate list is judgement] -->

- [person-directory](../features/person-directory.md) — for the Kshetra scope and the retired-kind guard.
- [role-appointment](../features/role-appointment.md) — for the containment walk the appointment ladder reads.
- [home-sabha-transfer](../features/home-sabha-transfer.md) — for the retired-kind guard on the destination.

All three reach this unit only through `StructuralHierarchyLookup`. The capabilities this unit
*owns* — structural creation, structural deletion, Sabha Kind lifecycle — still have no `features/`
page. All three are dossier candidates.

## Method

- Class listing plus a mapping-annotation grep over `sabha-service/**` — the primary method here, since all five `package-info.java` files are ADR-0019 ring scaffolds and carry zero information.
- `Data` → Owns is inferred from a writer-SQL grep over `sabha-data-access`, which is why that section earns the thinnest tag on the page.
