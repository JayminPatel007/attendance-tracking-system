---
kind: structure
slug: backend-container
source_paths: [apps/backend/application-container/**]
decisions: [ADR-0014, ADR-0015, ADR-0016, ADR-0019, ADR-0021, ADR-0022]
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

Flat — no ring, because there is no domain here. Ten classes plus resources.

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

## Sources

- [ADR-0014](../../adr/0014-monorepo-and-framework-scaffolding.md), [ADR-0015](../../adr/0015-bounded-context-seams-as-build-modules.md), [ADR-0016](../../adr/0016-oidc-auth-via-keycloak.md), [ADR-0019](../../adr/0019-bounded-context-module-taxonomy.md), [ADR-0021](../../adr/0021-spring-scheduling-for-occurrence-cron.md), [ADR-0022](../../adr/0022-web-session-via-bff-http-only-cookie.md)
- `apps/backend/application-container/src/main/resources/db/changelog/` — the whole schema
- `GlobalExceptionHandler.java` — the RFC 9457 contract and the `code` discriminator
