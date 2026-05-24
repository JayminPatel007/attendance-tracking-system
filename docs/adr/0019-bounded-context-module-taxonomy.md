# Bounded-context module taxonomy: five modules per context, presentation split from application service

**Status**: accepted. **Supersedes [ADR-0015](0015-bounded-context-seams-as-build-modules.md), [ADR-0017](0017-rest-adapters-live-in-application-modules.md), and [ADR-0018](0018-application-service-split.md).**

The 14-module layout from ADR-0015 (then 18 after ADR-0018) blurred two distinctions that DDD/Clean Architecture treats as separate: **presentation** (the HTTP transport surface) and **application services** (use-case orchestration). ADR-0017 made `*-application` hold both REST controllers *and* use cases; ADR-0018 then split application services by "single vs cross aggregate", which is a different cut than the one we actually want. This ADR resets the layout so the module names match the layer names from DDD and Clean Architecture, and so each module's boundary corresponds to a single Clean-Architecture ring.

## Layout per platform — backend (Spring Boot, Maven)

```
apps/backend/
├── pom.xml                                          <- backend-parent (packaging=pom)
├── common-domain/                                   <- cross-context VOs, ports,
│                                                       AggregateRoot, DomainEvent,
│                                                       domain exception base classes
├── identity-service/                                <- aggregator pom
│   ├── pom.xml
│   ├── identity-domain/                             <- aggregator pom (the hexagon)
│   │   ├── pom.xml
│   │   ├── identity-domain-core/                    <- aggregates, entities, VOs,
│   │   │                                               domain events, domain services
│   │   └── identity-application-service/            <- use-case orchestrators,
│   │                                                   driven-port interfaces
│   ├── identity-data-access/                        <- JDBC repository adapters
│   ├── identity-messaging/                          <- messaging adapters (may be empty)
│   └── identity-application/                        <- REST controllers + request/response DTOs
├── sabha-service/                                   <- same shape
├── attendance-service/                              <- same shape
├── analytics-service/                               <- same shape
└── application-container/                           <- @SpringBootApplication, application.yml,
                                                        Liquibase migrations, SecurityFilterChain,
                                                        global @RestControllerAdvice + ErrorResponse,
                                                        integration tests; the only fat jar
```

**Per bounded context: 5 leaf modules + 2 aggregator poms.** Four contexts × 7 = 28 poms, plus `common-domain`, `application-container`, and the root parent = **31 poms / 22 jar artifacts** across the backend.

Modules at every grouping level (including `*-service/` and `*-service/*-domain/`) are real Maven aggregator poms, so you can build a single context with `mvn -f attendance-service/pom.xml verify` or just its hexagon with `mvn -f attendance-service/attendance-domain/pom.xml verify`.

The old top-level `bootstrap` module is **renamed to `application-container`**. The old top-level `shared-kernel` module is **renamed to `common-domain`**. The dropped `common-application` module from the initial sketch is intentional: the only shared presentation code (global exception handler, error DTO) lives in `application-container` directly — see Q11.3 in the design discussion that produced this ADR.

## Mapping to Clean Architecture rings

| Clean ring                     | Maven module                       | Spring dependencies                                                                                                       |
| ------------------------------ | ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| Entities (innermost)           | `*-domain-core`, `common-domain`   | **None.** Pure Java.                                                                                                      |
| Use cases                      | `*-application-service`            | `spring-context` (`@Service`, `@Component`), `spring-tx` (`@Transactional`). **Nothing else.**                            |
| Interface adapters             | `*-data-access`, `*-messaging`, `*-application` | Each module pulls only the adapter libs it needs — JDBC / messaging client / Spring Web + Spring Security web annotations.|
| Frameworks & drivers (outer)   | `application-container`            | Spring Boot starters (web, actuator, jdbc, oauth2-resource-server, security), Liquibase, the runtime.                     |

The grouping `*-service/*-domain/{*-domain-core, *-application-service}` represents **the hexagon**: pure-Java aggregates plus their use-case orchestrators. The three sibling modules (`*-data-access`, `*-messaging`, `*-application`) are adapters around it.

## Dependency rules (compile-enforced)

- `common-domain` depends on nothing in the project. Pure Java.
- `*-domain-core` depends only on `common-domain`. Pure Java, zero Spring.
- `*-application-service` depends only on its own `*-domain-core` and `common-domain`. Permitted Spring: `spring-context`, `spring-tx`. **Forbidden:** `spring-web*`, `spring-jdbc`, `spring-data-*`, `spring-security-*`, any Spring Boot starter, any other context's modules.
- `*-data-access` depends only on its own `*-application-service` (transitively `*-domain-core` + `common-domain`). JDBC / JPA / `spring-jdbc` / `spring-data-*` OK.
- `*-messaging` depends only on its own `*-application-service`. Messaging client libs (Kafka, RabbitMQ, AWS SQS — none today) OK.
- `*-application` depends only on its own `*-application-service`. `spring-web`, `spring-webmvc`, `spring-security-web`, `spring-security-oauth2-jose`, `spring-security-oauth2-resource-server` OK for `@RestController`, `@AuthenticationPrincipal`, `Jwt`, etc.
- `application-container` depends on every leaf module plus Spring Boot starters.
- **Cross-context dependencies are forbidden.** Communication between contexts goes through ports declared in `common-domain` (e.g. `CallerResolver`) or through domain events. A `*-application-service` cannot import another context's `*-domain-core`.

The compiler — not code review — refuses to compile a violation, because the Maven module graph doesn't include the forbidden dependency.

## Where specific things live

- **Driven ports (repositories, event publisher, external service interfaces)** — in `*-application-service`, not `*-domain-core`. The application owns its own needs. Adapters in `*-data-access` / `*-messaging` implement them.
- **Cross-context ports** (e.g. `CallerResolver`) — in `common-domain`. Implementations live in the owning context's `*-data-access` (e.g. `identity-data-access` implements `CallerResolver` via a Keycloak-subject → local-user-id JDBC lookup).
- **DDD domain services** (stateless operations that don't fit on one aggregate) — in `*-domain-core` next to the aggregates. Pure functions; if data is needed, the application service loads it and passes it in. Domain services never call repositories directly.
- **Application services / use-case orchestrators** — concrete classes in `*-application-service`. No driving-port interfaces (no `interface MarkAttendanceUseCase` + `class MarkAttendanceUseCaseImpl` — single concrete class only). Reason: there is one driver (REST), so the interface adds no value, only ceremony.
- **DTOs** — request/response shapes live in `*-application` next to the controller that uses them. They are not domain types and they don't live in `*-domain-core`.
- **Domain exception base classes** (`DomainException`, `NotFoundException`, etc.) — in `common-domain`. Contexts throw subclasses; the global `@RestControllerAdvice` in `application-container` maps them to HTTP.
- **`SecurityFilterChain`, JWT decoder, OAuth2 resource-server config** — in `application-container`. Security is a deployment-wide composition concern, not a feature of identity.
- **Spring Boot main class, `application.yml`, Liquibase migrations, integration tests** — in `application-container`. The container holds *no feature code* with one declared exception: the global `@RestControllerAdvice` and `ErrorResponse` DTO live here too, because the HTTP error shape is a deployment-tier concern.

## Scaffolding policy

All 5 modules are scaffolded per context **even when empty** (e.g. `identity-messaging` ships with only a `pom.xml` and a `package-info.java` until a slice needs it). Rationale: the cost of an empty module is one pom and a docstring; the cost of restructuring the build the first time a contributor needs messaging in a new context is meaningful and easy to do incorrectly. ADR-0014's "pay the scaffolding cost up front" principle continues to apply.

## What changes from the superseded ADRs

| Old ADR                                           | What it said                                                                                                  | Now                                                                                                                                |
| ------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| **ADR-0015**                                      | 3 modules per context (`*-domain` / `*-application` / `*-infrastructure`), `*-application` is pure Java        | 5 modules per context, hexagon split into `*-domain-core` + `*-application-service`, adapters split into data-access / messaging / application |
| **ADR-0017**                                      | REST controllers live in `*-application` alongside use cases                                                  | REST controllers live in `*-application` (presentation only); use cases live in `*-application-service`. Same module name, different contents |
| **ADR-0018**                                      | Application tier split by "single vs cross aggregate" into `*-application` + `*-application-service`           | The split is by *layer* (presentation vs orchestration), not by aggregate count. "Cross-aggregate" is no longer a build-graph signal |
| `bootstrap`                                       | The fat-jar deployable                                                                                        | Renamed `application-container`. Same role; clearer name                                                                            |
| `shared-kernel`                                   | Cross-context VOs, kernel ports                                                                               | Renamed `common-domain`. Same role; clearer name                                                                                    |

What does **not** change from the superseded ADRs:

- One bounded context per Spring Boot deployable (ADR-0008's substantive claim survives).
- Hexagonal layering within each context.
- Cross-context dependencies still go through `common-domain` or domain events.
- Analytics read models are still projections, not joins, fed by domain events.
- Web and mobile platform layouts (Angular libraries, melos packages) are unchanged by this ADR — it is backend-only.

## Consequences

- 14 → 22 leaf modules on the backend, plus 8 aggregator poms. The existing `attendance-*`, `identity-*`, `sabha-*`, `analytics-*` modules from ADR-0015 must be split apart and renamed. `bootstrap` → `application-container`; `shared-kernel` → `common-domain`.
- The current code on `slice-2b-roster-marking` (`MarkAttendanceUseCase` in `attendance-application`, `JdbcOccurrenceRepository` in `attendance-infrastructure`) lives in modules that no longer exist by the new names. Migration is a separate PR — this ADR records the target shape only.
- Build wall-time grows modestly (more module graph traversal); offset by the ability to build a single context with `mvn -f *-service/pom.xml`.
- The "anemic domain" pattern currently in `attendance-domain` (`Occurrence` as a record with logic in `MarkAttendanceUseCase`) is incompatible with the rules above — the refactor is implementation of [ADR-0020](0020-aggregate-root-and-domain-events.md), not a separate decision.
- ADRs 0015, 0017, and 0018 stay in the tree marked `Status: superseded by ADR-0019` so the trail of *why* the layout evolved is preserved.
