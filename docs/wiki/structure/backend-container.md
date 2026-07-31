---
kind: structure
slug: backend-container
source_paths: [apps/backend/application-container/**]
decisions: [ADR-0008, ADR-0014, ADR-0016, ADR-0019, ADR-0021, ADR-0022]
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---
# Backend — Application Container

## Purpose

<!-- [coverage: high] -->

The single Spring Boot deployable (ADR-0008: one modular monolith, not four services). It owns
everything that must exist exactly once for the whole backend — security config, the global
exception handler, the clock, cron entry points, the Liquibase changelog, and the OpenAPI
definition. It holds no domain logic.

## Layout

<!-- [coverage: high] -->

One flat package (`org.sabha.container`), ten classes, plus `src/main/resources`:

| Class | Responsibility |
|---|---|
| `BackendApplication` | Boot entry point; pins the OpenAPI title/version so the committed spec stays stable for the drift gate (issue #73). |
| `SecurityConfig` | OIDC/Keycloak wiring (ADR-0016) and the BFF cookie session (ADR-0022). |
| `GlobalExceptionHandler` | The RFC 9457 Problem Details contract for every context. |
| `OpenApiConfig` | springdoc customisation, including the ring-split required-fields rule. |
| `ClockConfig` | The application-wide `Clock` bean — injected rather than `Instant.now()`, so tests can drive cron transitions without wall-clock waits. |
| `OccurrenceCronJobs` | Enables Spring scheduling globally; drives auto-Open and auto-Finalize (ADR-0021). |
| `AnalyticsCronJobs` | Rebuilds the re-engagement projection on a background cadence. |
| `LoggingDomainEventPublisher` | The only `DomainEventPublisher` implementation — logs at INFO and does nothing else. |
| `LoginActivityListener` | Records last-login on web sign-in, feeding the proxy picker's "last seen" hint. |
| `MkBootstrapRunner` | Seeds the first Madhyastha Karyalaya member at install time from env vars. |

`src/main/resources/db/changelog` holds the Liquibase changelog, **partitioned by slice/issue**
(`slice-2` … `slice-19`, `issue-77`, `issue-87`, `issue-89`) rather than by context.

## Exposes

<!-- [coverage: medium -- derived from `SecurityConfig`'s presence and the web proxy config, not from reading the filter chain. ] -->

No controllers of its own. It serves the OIDC endpoints Spring Security contributes —
`/oauth2/*`, `/login`, `/logout` — which the web dev proxy forwards alongside `/bff`. Every `/api/*`
and `/bff/*` route comes from a context module (ADR-0017).

## Talks To

<!-- [coverage: medium -- the cron edges are stated in the two CronJobs classes' Javadoc; the full bean-wiring graph was not traced. ] -->

**Outbound** — it is the composition root, so it depends on all four contexts at compile time.
Its live edges are the cron entry points into [[backend-attendance]]'s scanners and
[[backend-analytics]]'s projection scanner, and the `LoginActivityListener` into
[[backend-identity]]'s `UserActivityRecorder`.

**Inbound** — one common-domain port implemented here: `DomainEventPublisher`
(`LoggingDomainEventPublisher`). It lives here rather than in a context because there is exactly one
of it for the whole application.

## Data

<!-- [coverage: medium -- the changelog's location and partitioning are directly observable; the claim that no table is *owned* here is an inference from the absence of adapters. ] -->

Owns **no tables**, but owns the **schema**: every migration in the system lives under
`db/changelog`, and `db.changelog-master.yaml` is its include order.

This is the reason the wiki needs a content-based migration rule rather than a path glob — see
`protocol.md` §8. A `.sql` file landing here is invisible to any context page's `source_paths`.

## Gotchas

<!-- [coverage: medium -- the cron/scheduling coupling is stated in `AnalyticsCronJobs`' own Javadoc; the failure mode described is the compiler's inference from it. ] -->

`AnalyticsCronJobs` does **not** enable Spring scheduling — `OccurrenceCronJobs` does, globally, and
`AnalyticsCronJobs` free-rides on it. Deleting or disabling `OccurrenceCronJobs` silently stops the
analytics projection too.

`LoggingDomainEventPublisher` means **domain events currently have no subscribers**. Publishing an
event from an aggregate is real, but nothing downstream reacts; do not model a cross-context flow as
"it will pick up the event."

## Covered by

<!-- [coverage: low -- no feature dossiers exist yet; structurally empty until the first one is admitted. ] -->

_none_

## Sources

- [application-container](../../../apps/backend/application-container) — class inventory and per-class Javadoc
- [db/changelog](../../../apps/backend/application-container/src/main/resources/db/changelog) — slice/issue partitioning
- [ADR-0008](../../adr/0008-single-bounded-context-with-internal-seams.md), [ADR-0016](../../adr/0016-oidc-auth-via-keycloak.md), [ADR-0021](../../adr/0021-spring-scheduling-for-occurrence-cron.md), [ADR-0022](../../adr/0022-web-session-via-bff-http-only-cookie.md)
