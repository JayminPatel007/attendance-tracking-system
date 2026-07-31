---
kind: structure
slug: backend-common-domain
source_paths: [apps/backend/common-domain/**]
decisions: [ADR-0008, ADR-0015, ADR-0019, ADR-0020, ADR-0027, ADR-0029]
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---
# Backend — Common Domain

## Purpose

<!-- [coverage: high] -->

The one module every bounded context may depend on, and which may depend on none of them. It holds
the cross-context vocabulary — value objects, the `AggregateRoot` base, the domain-exception
hierarchy — and, most importantly, the **port interfaces through which contexts call each other**.
Per its own `package-info.java`: anything landing here must be genuinely cross-context; when in
doubt, leave it in the context that originated it.

## Layout

<!-- [coverage: high] -->

A single flat Maven module, one package (`org.sabha.common`), 37 files. No ring split — it is
entirely entities-ring material. Four groups:

| Group | Members |
|---|---|
| Cross-context ports | `CallerResolver`, `RoleAssignmentLookup`, `SantLookup`, `StructuralHierarchyLookup`, `SabhaShapeLookup`, `SabhaScheduleLookup`, `WeeklySabhaCatalog`, `SabhaProvisioning`, `MadhyasthaKaryalayaLookup`, `NirdeshakScopeLookup`, `NirikshakAssignmentLookup`, `RegionalTeamCityLookup`, `SanyojakZoneLookup`, `UserActivityRecorder`, `AuditReadAccess`, `DomainEventPublisher` |
| Value objects | `SabhaScope`, `SabhaSchedule`, `SabhaKind`, `WeeklySabhaRef`, `Role`, `OversightRole`, `VisibilityTier`, `CallerVisibility`, `AuthorizedAction`, `WhereClause` |
| Bases | `AggregateRoot`, `DomainEvent` |
| Exceptions | `DomainException`, `NotFoundException`, `ConflictException`, `AuthorizationDeniedException`, `CallerUnknownException`, `OptimisticLockException`, `ConcurrentModificationException`, `SabhaKindRetiredException` |

Pure Java: no Spring, no JPA, no framework dependency of any kind.

## Exposes

<!-- [coverage: high] -->

_none_

No HTTP surface — this module has no controllers and is not deployable on its own.

## Talks To

<!-- [coverage: high] -->

**Outbound** — _none_, by construction. ADR-0019 forbids common-domain from depending on any
bounded context, and there is nothing here to depend on it with.

**Inbound** — all four contexts. The ports declared here are implemented as `Jdbc*` adapters in
[[backend-identity]] (nine), [[backend-sabha]] (five), [[backend-analytics]] (one), and in
[[backend-container]] (`DomainEventPublisher`). [[backend-attendance]] implements none.

## Data

<!-- [coverage: high] -->

_none_

No persistence. `WhereClause` is a query-fragment value object, not a table binding.

## Gotchas

<!-- [coverage: medium -- the growth pressure is visible in the port count and stated as a risk in the module's own Javadoc; the ADR-0027 consequence is an inference from that ADR's title and this module's contents. ] -->

Sixteen of the 37 files are cross-context ports. Each one is a place a context reaches into another
without a compile-time dependency, so this module is where the modular monolith's seams either hold
or quietly dissolve — adding a port here is a coupling decision, not a refactor.

Deliberately **absent**: a shared granted-scope or permission module. ADR-0027 keeps each context's
authorization engine its own; `Role`, `OversightRole` and `AuthorizedAction` are vocabulary the
engines share, not an engine. ADR-0029 governs who may read `role_assignments` behind
`RoleAssignmentLookup`.

## Covered by

<!-- [coverage: low -- no feature dossiers exist yet; structurally empty until the first one is admitted. ] -->

_none_

## Sources

- [common-domain](../../../apps/backend/common-domain) — full file inventory and `package-info.java`
- [ADR-0015](../../adr/0015-bounded-context-seams-as-build-modules.md), [ADR-0019](../../adr/0019-bounded-context-module-taxonomy.md), [ADR-0027](../../adr/0027-no-shared-granted-scope-module-behind-the-authorization-engines.md), [ADR-0029](../../adr/0029-role-assignments-access-rule.md)
