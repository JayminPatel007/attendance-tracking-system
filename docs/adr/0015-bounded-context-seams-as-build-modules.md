# Bounded-Context Seams Are Build Modules (DDD + Hexagonal + Clean)

**Status**: accepted. **Supersedes [ADR-0008](0008-single-bounded-context-with-internal-seams.md).**

The system is still one bounded context per the original ADR-0008 reasoning (single org, presumably small team, modest concurrency). What changes here is the *enforcement mechanism* for the internal seams: they move from review-enforced Java packages to **build-tool-enforced modules**. Each bounded context — `identity`, `sabha`, `attendance`, `analytics` — is split into three modules (`domain`, `application`, `infrastructure`) on each platform, plus a `shared-kernel` shared by all and a single `bootstrap` deployable per platform.

The compiler — not code review — refuses to compile a reach-in.

## Why supersede 0008 rather than amend it

ADR-0008 accepted *review-enforced* boundaries as a deliberate tradeoff:

> Reach-ins (e.g., an attendance service grabbing a Person's repository directly) are prohibited by code review, not enforced by tooling — accepted tradeoff for now.

That tradeoff was load-bearing on the assumption that the team would catch reach-ins in review before they accumulated. After looking at the projected slice catalogue (Slices 1–19, several of them simultaneously editing multiple contexts) and at the deliberate AI-agent assistance model in this project, we judged that a reach-in is more likely to be merged accidentally than caught. The cost of formalising the seams as Maven modules / Angular libraries / Dart packages is one-time scaffolding; the cost of unwinding accidental coupling is per-incident and grows over time. Flipping the default — make the impossible-to-compile case the cheap one — is worth the upfront ceremony.

The original ADR is preserved in history so the *why we tried review-first* trail isn't lost; this one supersedes the enforcement decision while keeping the substantive claim (one bounded context, hexagonal layering within, analytics as the most likely extraction candidate) intact.

## Layout per platform

### Backend (Spring Boot, Maven)

```
apps/backend/
  pom.xml                              <- parent (packaging=pom)
  shared-kernel/                       <- pure Java, no Spring
  identity-domain/                     <- pure Java, depends on shared-kernel
  identity-application/                <- pure Java, depends on identity-domain
  identity-infrastructure/             <- Spring OK; adapters
  sabha-domain/ ... sabha-infrastructure/
  attendance-domain/ ... attendance-infrastructure/
  analytics-domain/ ... analytics-infrastructure/
  bootstrap/                           <- SpringBootApplication, application.yml,
                                          actuator, jdbc; the only fat jar
```

14 Maven modules. The dependency rules are enforced by `pom.xml`:

- `*-domain` depends *only* on `shared-kernel`.
- `*-application` depends only on its own `*-domain` and `shared-kernel`.
- `*-infrastructure` depends only on its own `*-application` (transitively the domain + shared-kernel) plus Spring.
- `bootstrap` depends on every `*-infrastructure` module and the Spring Boot starters it needs.
- Cross-context dependencies between bounded contexts go through **shared-kernel** or **domain events** — never directly module-to-module.

The Spring Boot main class lives in `bootstrap` and declares `@SpringBootApplication(scanBasePackages = "org.sabha")` so DI picks up adapters across every `*-infrastructure` module.

### Web (Angular workspace)

```
apps/web/
  angular.json                         <- workspace with one app + 7 libraries
  src/app/                             <- the Angular app (smart, routed)
  projects/
    shared-kernel/                     <- cross-context VOs
    shared-ui/                         <- presentational components
    shared-data-access/                <- HTTP client wrapper
    identity-domain/                   <- types/interfaces only
    sabha-domain/
    attendance-domain/
    analytics-domain/
```

Each library is an `ng-packagr` project with its own `public-api.ts`. The web's hexagonal flavour:

- `*-domain` libraries hold types only; no Angular dependency.
- A future `*-data-access` library per context wraps the HTTP adapters.
- A future `*-feature` library per context (or per feature) holds smart components and routes.
- The app at `src/app/` is the composition root — like backend `bootstrap`.

Mobile does not need `analytics` (web-tier per ADR-0003); web has the full set.

### Mobile (Flutter + melos)

```
apps/mobile/
  melos.yaml + pubspec.yaml            <- workspace root
  sabha_attendance/                    <- the Flutter app
  packages/
    shared_kernel/                     <- pure Dart, no Flutter dep
    identity_domain/
    sabha_domain/
    attendance_domain/
```

Each `packages/<x>/pubspec.yaml` declares its dependencies. The app's `pubspec.yaml` `path:`-depends on the four packages. `melos bootstrap` links them; `melos run analyze` / `melos run test` fan out across every package.

Analytics is deliberately omitted from mobile — ADR-0003 reserves analytics for the web tier.

## What this buys

1. **No reach-ins by accident.** An attendance use-case importing from `sabha-domain.application.MaterializeOccurrences` *does not compile* — there is no Maven dependency that lets it. Same on web (no path in `tsconfig.json`) and mobile (no entry in `pubspec.yaml`).
2. **Forced honesty on the shared kernel.** Anything that ends up in `shared-kernel` is genuinely cross-context — when two contexts both want a type, the move is to land it in the kernel, which means we'll think hard about whether it belongs there.
3. **Cheap future extraction.** Lifting `analytics` into its own service (the candidate flagged by 0008/0015) becomes a build-system change — rewire `analytics-infrastructure` to depend on a new HTTP client instead of in-process — not a code archaeology project.
4. **Symmetry across platforms.** The same context taxonomy appears on backend, web, and mobile, which lets a slice issue ("Slice 8 — Verified Home Sabha Transfer") map directly to a known set of modules on each platform.

## What this costs

- **Scaffolding ceremony.** ~14 Maven modules + ~7 Angular libraries + ~4 Dart packages = ~25 placeholder directories at Slice 1, most of them empty. Each costs a `pom.xml` / `ng-package.json` / `pubspec.yaml` and a docstring. We accept this as one-time cost.
- **Build orchestration.** Builds and tests must run in dependency order. Backend: `mvn verify` at the parent handles this automatically. Web: `npm run build:libs` then `ng build web`. Mobile: `melos bootstrap` then per-package commands.
- **Cross-context communication is more deliberate.** Where the old layout let `attendance.service` quietly call `sabha.repository`, the new layout forces the same code to either define an outbound port (cleaner) or push a type into `shared-kernel` (loud, considered). This is the *point*, not a downside.

## What does *not* change from 0008

- One bounded context, one Spring Boot deployable on the backend.
- Hexagonal layering applies *within* each context.
- Analytics read models are projections, not joins. Slices feeding analytics dispatch domain events that `analytics-infrastructure` projects into read models.
- The geographic structure (State → City → Zone → Kshetra → Sabha) is universal and lives in `shared-kernel`.

## Consequences

- A new feature inside a context lands in that context's three modules. A new feature touching two contexts adds an outbound-port abstraction in one and an inbound adapter in the other.
- New contexts (unlikely but possible) get their own `*-domain/application/infrastructure` triple on each platform.
- ArchUnit / Spring Modulith remain optional; the build-tool enforcement is the floor, those are extra (consider when team size grows).
- ADR-0014 (the original Slice-1 scaffolding ADR) is updated to reflect the module layout chosen here; the framework, monorepo, and CI decisions in that ADR stand unchanged.
