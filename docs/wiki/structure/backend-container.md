---
type: structure
title: Application Container
description: The Spring Boot composition root: the single deployable, the whole Liquibase schema, and the global error contract.
resource: apps/backend/application-container
aliases: [the Spring Boot app, the composition root, the schema]
source_paths: [
  apps/backend/application-container/src/main/**,
  apps/backend/application-container/pom.xml,
  docs/adr/0014-*.md,
  docs/adr/0015-*.md,
  docs/adr/0016-*.md,
  docs/adr/0019-*.md,
  docs/adr/0021-*.md,
  docs/adr/0022-*.md,
  CONTEXT.md
]
sources:
  - { id: adr-0014, title: "Monorepo, Angular Web, Spring Boot Backend Layout, and CI Structure", resource: ../../adr/0014-monorepo-and-framework-scaffolding.md }
  - { id: adr-0015, title: "Bounded-Context Seams Are Build Modules (DDD + Hexagonal + Clean)", resource: ../../adr/0015-bounded-context-seams-as-build-modules.md }
  - { id: adr-0016, title: "OIDC Authentication via Keycloak (Separate Container)", resource: ../../adr/0016-oidc-auth-via-keycloak.md }
  - { id: adr-0019, title: "Bounded-context module taxonomy: five modules per context, presentation split from application service", resource: ../../adr/0019-bounded-context-module-taxonomy.md }
  - { id: adr-0021, title: "Spring Scheduling for Occurrence cron jobs", resource: ../../adr/0021-spring-scheduling-for-occurrence-cron.md }
  - { id: adr-0022, title: "Web session via a Backend-for-Frontend with an HTTP-only cookie", resource: ../../adr/0022-web-session-via-bff-http-only-cookie.md }
  - { id: context, title: "CONTEXT.md", resource: ../../../CONTEXT.md }
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---

# Application Container

## Purpose

<!-- [coverage: high -- class listing + ADR-0015/0021] -->

The **composition root and the only deployable**. The four bounded contexts are libraries; this
module is the single Spring Boot application that assembles them, and it is the only unit permitted
to depend on all of them at once (ADR-0015).

It owns the four things that cannot belong to any one context: the security configuration, the
global error contract, the cron triggers, and the **entire database schema**.

## Layout

<!-- [coverage: high -- full file listing; only 10 main Java files] -->

Flat — no ring, because there is no domain here: a deviation from
[module-ring](../patterns/module-ring.md). Ten classes plus resources.

| Class | Role |
|---|---|
| `BackendApplication` | the Spring Boot entry point |
| `SecurityConfig` | OIDC resource server + the BFF cookie session (ADR-0016, ADR-0022) |
| `GlobalExceptionHandler` | the RFC 9457 `ProblemDetail` error contract for every context |
| `OccurrenceCronJobs`, `AnalyticsCronJobs` | `@Scheduled` triggers (ADR-0021) |
| `LoggingDomainEventPublisher` | the one implementation of common-domain's `DomainEventPublisher` |
| `LoginActivityListener` | records login into identity's `user_activity` |
| `MkBootstrapRunner` | env-var-driven Madhyastha Karyalaya bootstrap at startup |
| `ClockConfig`, `OpenApiConfig` | the injectable `Clock`; springdoc setup |

`src/main/resources/db/changelog` holds the **whole schema** — one master changelog plus ~26 SQL
files partitioned by **slice/issue**, not by context.

## Exposes

<!-- [coverage: high -- no controller in this module] -->

`_none_` — no controller of its own. It *serves* every route in the backend by hosting the four
contexts' controllers, but declares none. `GlobalExceptionHandler` shapes every one of those
responses without adding a route.

## Talks To

<!-- [coverage: high -- import scan; this is the one module allowed to import everything] -->

**Outbound** — everything. This is the only unit that imports contexts' packages **directly**
rather than through `org.sabha.common`, and that is exactly its job:

| Target | What it reaches for |
|---|---|
| identity | `MkBootstrapService`, `AddPersonApplicationService`, `OtpGuardedFlow`, `IdentityProviderGateway`, the directory use cases, `Gender`, `MobileAlreadyRegisteredException` |
| attendance | `AutoOpenScanner`, `AutoFinalizeScanner`, `WeeklyMaterializationScanner` — wired into `OccurrenceCronJobs` |
| analytics | `ReEngagementProjectionScanner` — wired into `AnalyticsCronJobs` |
| common | `DomainEventPublisher`, `UserActivityRecorder`, and the exception hierarchy the handler maps |

Reading a direct `org.sabha.identity.*` import anywhere **else** in the backend is a seam violation;
here it is correct by construction.

**Inbound** — one port: `DomainEventPublisher`, implemented by `LoggingDomainEventPublisher`.

## Data

<!-- [coverage: high -- the changelog directory is in this unit's source_paths and was read directly] -->

Owns **no table's data**, but owns **every table's definition**. All 19 tables are created here:
`users`, `persons`, `role_assignments`, `home_sabhas`, `home_sabha_transfers`, `password_resets`,
`selection_nominations`, `user_activity`, `nirikshak_sabha_assignments`, `cities`, `zones`,
`kshetras`, `sabha_kinds`, `sabhas`, `occurrences`, `attendance_markings`,
`occurrence_state_transitions`, `analytics_thresholds`, `reengagement_candidates`.

This is the structural fact behind `protocol.md` §8's migration rule: because the changelog is
partitioned by slice/issue rather than by context, **no path glob on a context's page can reach its
own migrations**. The table-name grep exists solely to bridge that gap.

## Gotchas

<!-- [coverage: medium -- two verified from the changelog and the seed file; no exhaustive sweep] -->

- **The changelog is partitioned by slice, not by context.** `slice-12/001-sabha-definition.sql`
  creates tables three different contexts read. Don't infer table ownership from the directory it
  was created in — infer it from who writes it.
- `slice-14/002-seed.sql` is the **only** source of `nirikshak_sabha_assignments` rows. Two contexts
  read that table and nothing in production writes it.
- Cron classes live here rather than beside the scanners they trigger, so a scanner looks unused
  from inside its own context. Grep this module before concluding a scanner is dead code.

## Covered by

<!-- [coverage: low -- no dossier covers the composition root; it is crossed by all of them] -->

`_none_` — no `features/` page owns the container. It is crossed by every capability rather than
implementing one, so it may never acquire a dedicated dossier.

## Method

- `src/main/resources/db/changelog/` read as a directory tree — the whole schema, partitioned by slice, and the reason this unit holds DDL custody for tables it owns no data in.
- `GlobalExceptionHandler.java` for the RFC 9457 contract and the `code` discriminator; configuration classes for the composition wiring.
