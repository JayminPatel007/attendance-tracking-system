# Monorepo, Angular Web, Spring Boot Backend Layout, and CI Structure

The Slice-1 scaffold (#2) commits the system to a single Git monorepo containing three deployables under `apps/`: a Spring Boot backend, an Angular web app, and a Flutter mobile app. Per [ADR-0015](0015-bounded-context-seams-as-build-modules.md), each platform's bounded-context seams are encoded as **separate build modules** (Maven modules / Angular libraries / Dart packages), not just packages — the compiler enforces the no-reach-in rule. GitHub Actions runs one workflow per app, each scoped by `paths:` so an app's CI only fires on changes that touch it.

## Why a monorepo rather than three separate repos

The three apps share one domain model (`CONTEXT.md`) and one bounded context (ADR-0008). Cross-app changes — adding a field on the backend that the mobile and web clients both need to read — happen routinely in this codebase. Polyrepos turn those into multi-PR coordination exercises; a monorepo makes them atomic. Single-org / small-team (ADR-0005) means we don't gain anything from per-app release cadence yet, and the cost — one CI runner, shared `.github/workflows/`, a single ADR directory — is small.

The risk a monorepo carries is CI bloat (every push runs everything). We mitigate this with `paths:` filters per workflow, so a `apps/web/**` change does not build the backend or mobile pipelines.

## Why Angular for the web

The web app is the operational surface for every non-Sabha-level role: Sanyojak / Nirdeshak / Sant / MK doing structural admin, role appointment, analytics dashboards, and Occurrence reopen. That is a forms-and-tables app with deep navigation, role-scoped section visibility (ADR-0003), and several long-lived stateful screens — exactly the shape Angular's batteries-included model (DI, router, forms, CLI-generated test setup, RxJS for the dashboards) is opinionated about. The alternative we weighed seriously was React; the deciding factor was that Angular's one-framework-one-way constraint matches a single-team build better than React's library-soup tradeoffs, and we get headless component testing for free via the Karma + Jasmine config the CLI ships.

The mobile app's framework was pre-decided by ADR-0003 (Flutter for one codebase across iOS + Android). The backend was pre-decided as Spring Boot before this slice.

## Backend module layout

Per ADR-0015 the bounded context is single, but its internal seams are encoded as **separate Maven modules** — not just Java packages — so a reach-in does not compile. The scaffold ships 14 modules under `apps/backend/`:

```
backend-parent (pom)
├── shared-kernel               <- pure Java, cross-context VOs
├── identity-domain             <- pure Java, depends on shared-kernel
├── identity-application        <- pure Java, depends on identity-domain
├── identity-infrastructure     <- Spring OK; REST controllers, JPA repos
├── sabha-{domain,application,infrastructure}
├── attendance-{domain,application,infrastructure}
├── analytics-{domain,application,infrastructure}
└── bootstrap                   <- SpringBootApplication, application.yml,
                                   actuator, jdbc; the only fat jar
```

Hexagonal layering applies *within* each context — domain at the core, application services orchestrating via ports, infrastructure adapters at the edges. The dependency rules are written into each `pom.xml`:

- `*-domain` depends only on `shared-kernel`.
- `*-application` depends only on its own `*-domain` and `shared-kernel`.
- `*-infrastructure` depends only on its own `*-application` plus Spring.
- `bootstrap` depends on every `*-infrastructure` module.
- Cross-context communication goes through `shared-kernel` or domain events — never directly module-to-module.

The Spring Boot main class lives in `bootstrap` and declares `@SpringBootApplication(scanBasePackages = "org.sabha")` so DI picks up adapters across every infrastructure module.

## Web library layout

Per ADR-0015 the Angular workspace is split into the main app plus seven `ng-packagr` libraries under `apps/web/projects/`:

- `shared-kernel` / `shared-ui` / `shared-data-access` — cross-cutting.
- `identity-domain` / `sabha-domain` / `attendance-domain` / `analytics-domain` — frontend mirrors of the backend bounded contexts.

Each library has its own `public-api.ts`; the app composes them. `tsconfig.json` registers the libraries under `paths:` so the app imports them by name (`import { … } from 'identity-domain'`). `npm run build:libs` builds libraries in dependency order before the app builds.

Mobile gets `analytics` deliberately omitted (ADR-0003: mobile is Sanchalak/Sah-Sanchalak only); web has the full set including a future `*-feature` library per slice.

## Mobile package layout

Per ADR-0015 `apps/mobile/` is a melos workspace:

```
apps/mobile/
  melos.yaml + pubspec.yaml     <- workspace root
  sabha_attendance/             <- the Flutter app
  packages/
    shared_kernel/              <- pure Dart, no Flutter dep
    identity_domain/
    sabha_domain/
    attendance_domain/
```

The app's `pubspec.yaml` declares `path:` dependencies on the four packages; `melos bootstrap` links them. `melos run analyze` and `melos run test` fan out across every package.

## CI structure

Three workflows in `.github/workflows/`, one per app, all triggered on `push` and `pull_request` with `paths:` filters:

- `backend.yml` — JDK 21 (Temurin), `mvn -B -ntp verify` at `apps/backend/` (builds all 14 modules in dependency order). Maven cache via `actions/setup-java`.
- `web.yml` — Node 20, `npm ci`, `npm run build:libs` (ng-packagr for the seven libraries in dependency order), then `ng test web --browsers=ChromeHeadlessCI` and `ng build web --configuration=production`.
- `mobile.yml` — `subosito/flutter-action@v2`; `flutter create --platforms=android,ios --project-name=sabha_attendance_mobile .` inside `sabha_attendance/` to regenerate the gitignored platform dirs; `dart pub global activate melos`; `melos bootstrap`; `melos run analyze`; `melos run test`.

A repo-wide pipeline that fans out into the three jobs was considered. We deferred it: in practice each app's path-filtered workflow handles "did the thing I changed still build" cleanly, and a wrapper would mostly duplicate status reporting. If we add cross-app contract tests later they can live in a separate workflow without touching these three.

## Tests in the tracer

Each app ships one smoke test that proves its stack works end-to-end:

- Backend — `bootstrap/src/test/.../HealthEndpointTest` asserts `GET /actuator/health` returns `{status:UP, components.db.status:UP}`. Uses H2 for fast feedback; the real Postgres path is exercised by `docker compose up` and Slice 2's integration test (#3).
- Web — `src/app/app.component.spec.ts` asserts the home placeholder renders the app title. Runs headlessly via Karma + ChromeHeadlessCI.
- Mobile — `sabha_attendance/test/splash_test.dart` widget-tests that the splash screen renders the app title.

## Consequences

- All future slices land in `apps/{backend,web,mobile}` — slice issues should not propose new top-level deployables without an ADR.
- Adding code inside an existing bounded context means editing the right one of its three modules. Adding a *new* bounded context means adding a `*-domain/application/infrastructure` triple on each platform — capture in an ADR.
- Mobile platform directories live under `sabha_attendance/{android,ios,...}` and are not committed; CI regenerates them via `flutter create`. A developer cloning fresh runs `cd apps/mobile/sabha_attendance && flutter create --platforms=android,ios --project-name=sabha_attendance_mobile .` once before `flutter run`.
- Local Postgres for the running stack comes up via `docker compose up` and listens on host port **55432** to avoid clashing with any pre-existing host Postgres on 5432.
- Tests use H2; production and the compose stack use Postgres. The schema-migration story (Flyway / Liquibase) lands in Slice 2 (#3) where real tables first appear.
