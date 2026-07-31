---
kind: structure
slug: backend-sabha
source_paths: [apps/backend/sabha-service/**]
decisions: [ADR-0009, ADR-0012, ADR-0019, ADR-0024, ADR-0026, ADR-0028]
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---
# Backend — Sabha Context

## Purpose

<!-- [coverage: high] -->

The durable organisational furniture: the geographic hierarchy (City → Zone → Kshetra), the
extensible registry of Sabha Kinds, and the Sabha aggregate itself with its schedule shape and
standing venue. It owns creation, deletion and retirement of all of those — not the dated
gatherings, which are [[backend-attendance]]'s.

## Layout

<!-- [coverage: high] -->

Five Maven modules per ADR-0019:

| Module | Ring | What lives in it |
|---|---|---|
| `sabha-domain-core` | Entities | `City`, `Zone`, `Kshetra`, `Sabha`, `SabhaKind`, `Demographic`, `Track`, `ScheduleShape`, `StructuralNames`, and the structural exceptions. |
| `sabha-application-service` | Use cases | `StructuralCreationService`, `StructuralDeletionService`, `SabhaKindLifecycleService`, `StructuralScopeAuthority`, `StructuralQueries`, and the five repository ports. |
| `sabha-data-access` | Interface adapters | 11 `Jdbc*` adapters — five repositories plus the five common-domain lookups this context provides. |
| `sabha-messaging` | Interface adapters | Empty scaffold. |
| `sabha-application` | Interface adapters | `StructuralCreationController`, `StructuralDeletionController`, `SabhaListController`. |

No feature-package split: the module is small enough that the class names are the navigation.

## Exposes

<!-- [coverage: high] -->

**`/bff/*` only — this context has no mobile surface.** `/bff/structure/cities`, `/bff/structure/zones`,
`/bff/structure/kshetras` (each with `/{id}`), `/bff/structure/my-cities`, `/bff/structure/my-zones`,
`/bff/structure/sabha-kinds` (plus `/{id}/retire` and `/{id}/reactivate`), and
`/bff/sabhas/{id}` + `/bff/sabhas/mine`.

`POST /bff/sabhas` is *not* here — it belongs to [[backend-identity]]. See its Gotchas.

## Talks To

<!-- [coverage: medium -- edges derived from `import org.sabha.common.*` and `implements` scans; imports reached only from tests would read as live edges. ] -->

**Outbound** — authority and caller resolution, all into [[backend-identity]] via common-domain
ports: `CallerResolver`, `RoleAssignmentLookup`, `NirdeshakScopeLookup`, `MadhyasthaKaryalayaLookup`,
`RegionalTeamCityLookup`, `SanyojakZoneLookup`. `RegionalTeamCityLookup` is the ADR-0024 edge —
Zone creation moved from the Madhyastha Karyalaya to the Regional Team.

**Inbound** — five common-domain ports implemented here for the other contexts:
`StructuralHierarchyLookup`, `SabhaShapeLookup`, `SabhaScheduleLookup`, `WeeklySabhaCatalog`,
`SabhaProvisioning`. The last is the seam identity calls when a Sabha is defined.

## Data

<!-- [coverage: low -- ownership inferred from INSERT/UPDATE targets in this module's adapters; no schema-ownership manifest exists. Verify before acting. ] -->

Written here: `cities`, `zones`, `kshetras`, `sabhas`, `sabha_kinds`.

Read-only here: `occurrences` — only to answer "is this structural node empty?" for the ADR-0026
block-if-non-empty deletion rule.

## Gotchas

<!-- [coverage: medium -- the stale Javadoc is directly observable; that it is stale rather than describing a different package is the inference. ] -->

Every `package-info.java` in this context still reads *"Empty scaffold per ADR-0019"* — written when
the modules were scaffolded and never updated as ~50 classes landed. Do not take them as a statement
that the module is empty; they are the least reliable source in this context.

## Covered by

<!-- [coverage: low -- no feature dossiers exist yet; structurally empty until the first one is admitted. ] -->

_none_

## Sources

- [sabha-service](../../../apps/backend/sabha-service) — module layout and class inventory
- [ADR-0009](../../adr/0009-structural-creation-authority.md), [ADR-0012](../../adr/0012-sabha-schedule-shapes-and-occurrence-materialization.md), [ADR-0024](../../adr/0024-zone-creation-moves-to-regional-team.md), [ADR-0026](../../adr/0026-deletion-model.md)
- [CONTEXT.md](../../../CONTEXT.md) — Sabha, Sabha Type, Kshetra vocabulary
