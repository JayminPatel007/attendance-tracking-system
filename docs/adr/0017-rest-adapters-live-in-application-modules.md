# REST adapters live in `*-application` modules

**Status**: accepted. **Amends [ADR-0015](0015-bounded-context-seams-as-build-modules.md).**

ADR-0015 split each bounded context into `*-domain` / `*-application` / `*-infrastructure` and described `*-application` as "pure Java, no Spring." We are relaxing that one constraint: **`*-application` modules may use Spring Web and Spring Security annotations to expose HTTP endpoints alongside their use cases.** Outbound adapters (JDBC, messaging, external HTTP clients) remain in `*-infrastructure`.

## Why amend rather than keep strict

The strict reading of ADR-0015 leaves us with two unappealing options when a context needs HTTP:

1. Put `@RestController` in `*-infrastructure`. Then the HTTP-facing entry point is in the same module as the JDBC adapters that implement outbound ports — two different *kinds* of adapter sharing one module just because both have `@`-annotations. Reading the code, the inbound surface ("what HTTP endpoints does identity expose?") is buried next to the outbound surface ("how does identity persist users?").
2. Add a third adapter module per context (e.g. `identity-rest`, `attendance-rest`). That's four more modules at no behavioural gain — the inbound adapter is trivially short and tightly coupled to the use cases it delegates to.

Putting the REST controller next to the use case it invokes is how most teams actually read this kind of code: "GET /api/whoami → WhoAmIUseCase → UserRepository port → JDBC adapter". When the controller and use case are in the same module, the call site is one click away. When they're in different modules, you're routinely jumping module boundaries to read a single feature.

The cost ADR-0015 paid for "no Spring in application" was the ability to reuse those modules in non-Spring contexts (a CLI, a different framework). We have no such reuse plan — `bootstrap` is the only deployable on the backend, and the web/mobile platforms are governed by different ADRs entirely. So that cost is paying for an option we never plan to exercise.

## What `*-application` may now depend on

Permitted Spring artifacts in `*-application`:

- `org.springframework:spring-web` — `@RestController`, `@GetMapping`, `ResponseEntity`, etc.
- `org.springframework:spring-context` — `@Service`, `@Component`, DI annotations
- `org.springframework:spring-tx` — `@Transactional` (the annotation; the transaction manager itself is wired in `bootstrap`)
- `org.springframework.security:spring-security-core` — `@AuthenticationPrincipal`
- `org.springframework.security:spring-security-oauth2-jose` / `-resource-server` — `Jwt`, JWT-related types

`*-application` must NOT depend on:

- Spring Boot starters that pull in autoconfig or runtime (`spring-boot-starter-web`, `spring-boot-starter-jdbc`, `spring-boot-starter-security`). Those stay in `*-infrastructure` and `bootstrap`.
- Any persistence adapter (`spring-jdbc`, JPA, R2DBC).
- HTTP clients (`spring-webflux`, `RestClient`, `WebClient`).
- Other contexts' modules (the cross-context rule from ADR-0015 is unchanged).

The principle: **`*-application` knows about HTTP and use cases. It does not know about the database, messaging, or external services.** Those remain outbound adapters in `*-infrastructure`.

## What does *not* change from ADR-0015

- Module taxonomy: one bounded context = three modules per platform.
- `*-domain` is still pure Java with no Spring.
- `*-infrastructure` still holds JDBC adapters, security config (`SecurityFilterChain` beans), and any other framework-runtime concerns.
- Cross-context dependencies still go through `shared-kernel` or domain events.
- `bootstrap` is still the only deployable, the only fat jar, the only place `@SpringBootApplication` lives. **`bootstrap` does not house feature code** — when an integration smoke test in `bootstrap/src/test` finds itself drafting a controller, the controller goes into the appropriate `*-application`, not into `bootstrap`.

## Consequences

- The Slice-2 controllers `CurrentRosterController` and `MarkAttendanceController`, currently sitting in `bootstrap` as a tracer-slice compromise, move to `attendance-application` with use cases extracted into `GetCurrentRosterUseCase` / `MarkAttendanceUseCase`. The SQL moves into JDBC adapters in `attendance-infrastructure` behind ports defined in `attendance-domain`.
- `IdentityRestController` moves from `identity-infrastructure` to `identity-application`.
- Cross-context lookups (e.g. "resolve the JWT subject to a local `users.id`") use the new `CallerResolver` port in `shared-kernel`, implemented in `identity-infrastructure`. Attendance's use cases depend on the port; they never reach into identity's domain types.
- The pom-level descriptions of each `*-application` module are updated to reflect this policy.
