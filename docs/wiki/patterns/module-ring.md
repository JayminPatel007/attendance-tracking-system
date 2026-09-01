---
type: pattern
title: Module Ring
description: The five-module Clean-Architecture ring every backend bounded context is built from, and what the Maven graph enforces.
aliases: [ring, the hexagon, five modules, Clean Architecture, domain-core, application-service, data-access]
source_paths: [
  apps/backend/pom.xml,
  apps/backend/*-service/pom.xml,
  apps/backend/*-domain/pom.xml,
  apps/backend/application-container/pom.xml,
  docs/adr/0015-*.md,
  docs/adr/0017-*.md,
  docs/adr/0018-*.md,
  docs/adr/0019-*.md,
  CONTEXT.md
]
sources:
  - { id: adr-0015, title: "Bounded-Context Seams Are Build Modules (DDD + Hexagonal + Clean)", resource: ../../adr/0015-bounded-context-seams-as-build-modules.md }
  - { id: adr-0017, title: "REST adapters live in `*-application` modules", resource: ../../adr/0017-rest-adapters-live-in-application-modules.md }
  - { id: adr-0018, title: "Application services split: `*-application` vs `*-application-service`", resource: ../../adr/0018-application-service-split.md }
  - { id: adr-0019, title: "Bounded-context module taxonomy: five modules per context, presentation split from application service", resource: ../../adr/0019-bounded-context-module-taxonomy.md }
appears_in: [backend-identity, backend-sabha, backend-attendance, backend-analytics, backend-common-domain, backend-container]
last_compiled: 18c0993c1c22d3217d62a879beed639914f74aee
---

# Module Ring

## The pattern

<!-- [coverage: high -- ADR-0019's layout and dependency-rule lists, cross-checked against the six backend structure pages] -->

Every backend bounded context is the **same five leaf Maven modules**, one per Clean-Architecture
ring, grouped by two aggregator poms. The names are mechanical: `<ctx>` is one of `identity`,
`sabha`, `attendance`, `analytics`.

| Module | Ring | Depends on | Spring permitted |
|---|---|---|---|
| `<ctx>-domain/<ctx>-domain-core` | entities | `common-domain` | **none** — pure Java |
| `<ctx>-domain/<ctx>-application-service` | use cases | its own `-domain-core` | `spring-context`, `spring-tx` |
| `<ctx>-data-access` | adapters | its own `-application-service` | JDBC only |
| `<ctx>-messaging` | adapters | its own `-application-service` | messaging clients only |
| `<ctx>-application` | adapters (presentation) | its own `-application-service` | Spring Web + Security web |

Three properties follow, and they are what makes the shape worth naming once:

- **The dependency direction is compile-enforced.** A forbidden edge is not in the Maven module
  graph, so it does not compile — the reason cross-context reach-ins cannot happen and the reason
  `-domain-core` cannot acquire a Spring dependency by accident.
- **Cross-context traffic leaves through `common-domain` ports**, implemented in the owning
  context's `-data-access`. That is the same seam [authorization](authorization.md) travels on.
- **All five ship even when empty.** `<ctx>-messaging` is often a lone `package-info.java`;
  ADR-0014's pay-the-scaffolding-cost-up-front principle is what keeps it there.

Because the ring is identical everywhere, each unit's page carries the same five rows and only its
`Holds` column says anything unit-specific.

## Why

<!-- [coverage: high -- ADR-0019 and the three ADRs it supersedes] -->

ADR-0019 is the settled shape and **supersedes ADR-0015, ADR-0017 and ADR-0018**; the trail is worth
keeping because the two boundaries it moved are the two a reader still gets wrong. ADR-0015 cut three
modules per context and put use cases in a pure-Java `*-application`. ADR-0017 then let that module
hold REST controllers *and* use cases. ADR-0018 split the application tier by *single-versus-cross
aggregate* — a real distinction, but not a ring boundary, so the module names stopped matching the
layer names. ADR-0019 recut the split by **layer**: presentation in `*-application`, orchestration in
`*-application-service`. `bootstrap` became `application-container` and `shared-kernel` became
`common-domain` in the same pass, so any older name in a comment is stale rather than a module you
have not found.

## Where it appears

<!-- [coverage: high -- the Layout section of each of the six backend structure pages] -->

| Page | How it instantiates the ring |
|---|---|
| [backend-identity](../structure/backend-identity.md) | the full five, and the only unit large enough that feature packages inside `-application-service` beat the ring as a navigation axis |
| [backend-sabha](../structure/backend-sabha.md) | the full five; one flat application-service package, so the ring *is* the navigation axis |
| [backend-attendance](../structure/backend-attendance.md) | the full five, unusually top-heavy — most of the unit sits in the use-case ring |
| [backend-analytics](../structure/backend-analytics.md) | the full five, with `analytics-messaging` an empty scaffold |
| [backend-common-domain](../structure/backend-common-domain.md) | **the innermost ring itself** — one flat module every context's ring depends on |
| [backend-container](../structure/backend-container.md) | **the outermost ring** — frameworks and drivers, depending on every leaf module |

## Deviations

<!-- [coverage: medium -- read off the two flat units' pages and the ArchUnit test's javadoc; not re-derived from the poms] -->

- **Two backend units have no ring at all**, and correctly so: `common-domain` is the entities ring
  as a single flat module, and `application-container` is the frameworks ring as a single flat
  module. Both are ring *positions*, not contexts, so there is nothing inside them to layer.
- **`application-container` holds one declared piece of feature code** — the global
  `@RestControllerAdvice` and the error DTO — because the HTTP error shape is a deployment-tier
  concern. ADR-0019 states the exception rather than leaving it as drift.
- **The intra-module conventions are not in the graph.** The compiler cannot see a controller
  injecting a repository, or `@Transactional` outside the use-case tier. Those are enforced instead
  by `IntraModuleArchitectureRulesTest` in `application-container`'s test tree (issue #69), which is
  where the package-to-ring mapping is actually written down.
- **Mobile and web are outside this pattern.** ADR-0019 is backend-only; the Melos packages and the
  `ng-packagr` libraries are governed by ADR-0014/0015 and share none of these rules.

## Method

- ADR-0019's Clean-ring table and its dependency-rules list are effectively the whole page; the six structure pages were read for `Deviations` only, and contributed nothing to `The pattern`.
- `IntraModuleArchitectureRulesTest`'s class javadoc is the one place the **package → ring** mapping exists — ADR-0019 states the mapping in Maven-module terms only, so a compiler working from the ADR alone cannot tell which package sits in which ring. Read it first.
