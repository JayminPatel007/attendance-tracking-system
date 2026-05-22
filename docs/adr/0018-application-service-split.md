# Application services split: `*-application` vs `*-application-service`

**Status**: superseded by [ADR-0019](0019-bounded-context-module-taxonomy.md). This ADR split `*-application` into `*-application` (single-aggregate use cases) + `*-application-service` (cross-aggregate orchestration). ADR-0019 keeps both module names but cuts the split along a **different axis**: `*-application` is now presentation-only (REST + DTOs); `*-application-service` holds **all** use-case orchestrators regardless of aggregate count. The "single vs cross aggregate" distinction this ADR encoded into the build graph is dropped — aggregate count is no longer a build-graph signal. See ADR-0019 for the current rules.

Each bounded context's application tier is split into two Maven modules:

- `*-application` — use cases that orchestrate **a single aggregate within this context** plus their REST entry points. Depends on `*-domain` and `shared-kernel`.
- `*-application-service` — use cases that orchestrate **multiple aggregates** (within or across contexts) plus their REST entry points. Depends on `*-domain`, and `shared-kernel`. Cross-context coordination happens through ports declared in `shared-kernel` (e.g. `CallerResolver`); direct dependencies on other contexts' modules are still forbidden by ADR-0015.

The split makes the *coordination cost* of a use case visible in the build graph: a class living in `attendance-application-service` is announcing that it touches more than one aggregate. Classes in `*-application` are pure inner loops of one aggregate.

## Why two modules and not one

A single `*-application` module conflates two qualitatively different things:

- **Single-aggregate interactors** — e.g. `WhoAmIUseCase` reads one `User`. The behaviour belongs on the aggregate; the use case is a one-line dispatch. Risk profile: low. Test boundary: the aggregate itself.
- **Cross-aggregate orchestrators** — e.g. `MarkAttendanceApplicationService` resolves the calling Sanchalak through a kernel port, loads the `Occurrence`, asks it to mark a `Person`, and persists the resulting `AttendanceMarking`. Risk profile: high (transactions, ordering, rollback, port wiring). Test boundary: integration or service-level.

Mixing them encourages putting domain logic inside the orchestrator (since the orchestrator already has dependencies on multiple repositories, "what's one more line of business rule?"). The split makes "you are now writing cross-aggregate code" a build-graph event.

This also gives a natural home for the application-service classes (DDD term) that ADR-0017 lumped into `*-application` for expediency.

## What lives where

| Module                         | Holds                                                                                | Spring deps |
| ------------------------------ | ------------------------------------------------------------------------------------ | ----------- |
| `*-domain`                     | Aggregate roots (entity classes), value objects, domain events, ports                | none        |
| `*-application`                | Single-aggregate use cases, REST controllers that expose them                        | `spring-web`, `spring-context`, `spring-tx`, `spring-security-core`, `spring-security-oauth2-jose` (per ADR-0017) |
| `*-application-service`        | Cross-aggregate orchestration, REST controllers that expose them                     | same as `*-application` |
| `*-infrastructure`             | Outbound adapters: JDBC repositories, security config, external clients              | Spring Boot starters as needed |
| `bootstrap`                    | `@SpringBootApplication`, `application.yml`, Liquibase migrations, integration tests | starters for web, actuator, jdbc, oauth2-resource-server |

REST controllers can live in either `*-application` or `*-application-service`, depending on which tier of use case they call. Controllers in `*-application-service` are allowed because their use cases need to be there anyway, and forcing the controller into `*-application` would create a back-reference from a lower layer to a higher one.

## Dependency direction (per context)

```
*-infrastructure ──> *-application-service ──> *-application ──> *-domain
                                       │                              ▲
                                       └──────────────────────────────┘
                                                  (also)
                shared-kernel ◀── every module
```

`*-application-service` depends on `*-application` so it can call single-aggregate use cases when needed (e.g. an orchestrator that first asks `WhoAmIUseCase` then loads an `Occurrence`). This is one-way — `*-application` does not depend on `*-application-service`.

## Slice-2 footprint

After this ADR:

- `attendance-application-service` holds `MarkAttendanceApplicationService`, `GetCurrentRosterApplicationService`, and `AttendanceRestController` (all three touch the `CallerResolver` kernel port, so they are cross-context).
- `identity-application` holds `IdentityRestController` (and a future `WhoAmIUseCase` if extracted) — single-aggregate, all within identity.
- `attendance-application`, `sabha-application`, `analytics-application`, and their `-application-service` siblings are scaffolded with package-info docstrings but otherwise empty until later slices populate them.

## What does *not* change

- One bounded context per ADR-0008 / 0015. The split is *intra*-context.
- Cross-context dependencies still go through `shared-kernel` (ports like `CallerResolver`) or domain events. `attendance-application-service` cannot import `identity-domain`.
- Aggregate boundaries unchanged; the rule for choosing application-service is "does this use case mutate two or more aggregates?", not "does it read from two contexts?". A read-only cross-context projection is fine in `*-application-service` because that's where the cross-context wiring naturally lands.
- The compiler-enforced reach-in prevention from ADR-0015 still holds.

## Consequences

- Backend module count rises from 14 to 18.
- Each bounded context has a clear "single vs cross aggregate" home for its use cases; new contributors don't have to guess.
- Build graph carries useful signal: a PR that touches `*-application-service` is doing coordination work and warrants a closer look at transactional boundaries and event ordering.
- The DDD vocabulary (aggregate, application service, value object, port) maps 1:1 to module + class shape: aggregates and ports in `*-domain`, application services in `*-application-service`, single-aggregate interactors in `*-application`.
