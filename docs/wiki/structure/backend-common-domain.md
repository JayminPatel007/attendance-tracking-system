---
kind: structure
slug: backend-common-domain
source_paths: [
  apps/backend/common-domain/src/main/**,
  apps/backend/common-domain/pom.xml,
  docs/adr/0008-*.md,
  docs/adr/0015-*.md,
  docs/adr/0019-*.md,
  docs/adr/0020-*.md,
  docs/adr/0027-*.md,
  CONTEXT.md
]
decisions: [ADR-0008, ADR-0015, ADR-0019, ADR-0020, ADR-0027]
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---

# Common Domain

## Purpose

<!-- [coverage: high -- package-info.java + ADR-0019 + a full import scan of all four contexts] -->

The **only** thing the four bounded contexts share. It holds cross-context value objects and
identifiers, the `AggregateRoot` base and the `DomainEvent` + publisher port, the domain-exception
base classes, and — the load-bearing part — the **cross-context ports** through which every context
reaches every other one. Pure Java: no Spring, no JPA, no framework dependency.

Per ADR-0019 every context may depend on common-domain and the converse is forbidden. The
package-info states the admission bar directly: anything landing here must be *genuinely*
cross-context, and when in doubt it stays in the context that originated it.

## Layout

<!-- [coverage: high -- directory listing; 37 main source files] -->

A **single flat module** with one package, `org.sabha.common` — no ring, because there is nothing
to ring. It is a leaf that the ring modules of all four contexts depend on.

| Group | Types |
|---|---|
| Aggregate + events | `AggregateRoot`, `DomainEvent`, `DomainEventPublisher` |
| Exception bases | `DomainException`, `NotFoundException`, `ConflictException`, `AuthorizationDeniedException`, `ConcurrentModificationException`, `OptimisticLockException`, `CallerUnknownException`, `SabhaKindRetiredException` |
| Authority vocabulary | `Role`, `OversightRole`, `AuthorizedAction`, `SabhaScope`, `CallerVisibility`, `VisibilityTier`, `AuditReadAccess` |
| Sabha vocabulary | `SabhaKind`, `SabhaSchedule`, `WeeklySabhaRef`, `WhereClause` |
| Cross-context ports | the 14 lookups and gateways listed under Talks To |

## Exposes

<!-- [coverage: high -- no `application` module exists in this unit] -->

`_none_` — common-domain has no ring, no adapters and no REST surface. It is a library, not a
deployable slice; the HTTP surface belongs to the four contexts.

## Talks To

<!-- [coverage: high -- import scan: zero `org.sabha.*` imports out of this module] -->

**Outbound** — `_none_`. Zero imports of any other `org.sabha` package. That is the invariant
ADR-0019 exists to protect, and it holds exactly.

**Inbound** — every other backend unit depends on this one. The ports declared here are
*interfaces only*; each is implemented in whichever context **owns** the data, and consumed by the
others. Implementers, by owning context:

| Implemented in | Ports |
|---|---|
| identity | `CallerResolver`, `RoleAssignmentLookup`, `SantLookup`, `MadhyasthaKaryalayaLookup`, `SanyojakZoneLookup`, `NirdeshakScopeLookup`, `NirikshakAssignmentLookup`, `RegionalTeamCityLookup`, `UserActivityRecorder` |
| sabha | `StructuralHierarchyLookup`, `SabhaProvisioning`, `SabhaScheduleLookup`, `SabhaShapeLookup`, `WeeklySabhaCatalog` |
| analytics | `AuditReadAccess` |
| application-container | `DomainEventPublisher` |

Read the table as the repo's whole cross-context wiring diagram: nine edges point at identity, five
at sabha, and attendance is a pure consumer that implements nothing.

## Data

<!-- [coverage: high -- no persistence dependency and no data-access module in this unit] -->

`_none_` — owns no table and touches no database. `SabhaScope`, `SabhaKind` and friends are value
objects; the rows behind them belong to the context that implements the corresponding port.

## Gotchas

<!-- [coverage: medium -- shape from ADR-0027 and the port table; no exhaustive audit of what "belongs" here] -->

- ADR-0027 is a **negative** decision that shapes this module: there is deliberately **no** shared
  granted-scope module behind the per-context authorization engines. Each context keeps its own
  engine and reaches here only for the raw lookups. Resist the pull to hoist an
  `AuthorizationEngine` into common-domain — that has already been decided against.
- `SabhaKind` exists **twice**: `org.sabha.common.SabhaKind` (the cross-context value) and
  `org.sabha.sabha.domain.SabhaKind` (the aggregate sabha writes). Same name, different types.

## Covered by

<!-- [coverage: low -- one dossier exists so far; this unit is crossed by nearly every capability] -->

- [[attendance-marking]]

## Sources

- [ADR-0008](../../adr/0008-single-bounded-context-with-internal-seams.md), [ADR-0015](../../adr/0015-bounded-context-seams-as-build-modules.md), [ADR-0019](../../adr/0019-bounded-context-module-taxonomy.md), [ADR-0020](../../adr/0020-aggregate-root-and-domain-events.md), [ADR-0027](../../adr/0027-no-shared-granted-scope-module-behind-the-authorization-engines.md)
- `apps/backend/common-domain/src/main/java/org/sabha/common/package-info.java`
- Import scan across all six backend units — the evidence for both halves of Talks To
