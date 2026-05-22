# Monorepo, Angular Web, Spring Boot Backend Layout, and CI Structure

The Slice-1 scaffold (#2) commits the system to a single Git monorepo containing three deployables under `apps/`: a Spring Boot backend, an Angular web app, and a Flutter mobile app. Backend Java is laid out by the seams declared in ADR-0008 — `org.sabha.identity`, `org.sabha.sabha`, `org.sabha.attendance`, `org.sabha.analytics` — with a `BackendApplication` entry point at the package root. GitHub Actions runs one workflow per app, each scoped by `paths:` so an app's CI only fires on changes that touch it.

## Why a monorepo rather than three separate repos

The three apps share one domain model (`CONTEXT.md`) and one bounded context (ADR-0008). Cross-app changes — adding a field on the backend that the mobile and web clients both need to read — happen routinely in this codebase. Polyrepos turn those into multi-PR coordination exercises; a monorepo makes them atomic. Single-org / small-team (ADR-0005) means we don't gain anything from per-app release cadence yet, and the cost — one CI runner, shared `.github/workflows/`, a single ADR directory — is small.

The risk a monorepo carries is CI bloat (every push runs everything). We mitigate this with `paths:` filters per workflow, so a `apps/web/**` change does not build the backend or mobile pipelines.

## Why Angular for the web

The web app is the operational surface for every non-Sabha-level role: Sanyojak / Nirdeshak / Sant / MK doing structural admin, role appointment, analytics dashboards, and Occurrence reopen. That is a forms-and-tables app with deep navigation, role-scoped section visibility (ADR-0003), and several long-lived stateful screens — exactly the shape Angular's batteries-included model (DI, router, forms, CLI-generated test setup, RxJS for the dashboards) is opinionated about. The alternative we weighed seriously was React; the deciding factor was that Angular's one-framework-one-way constraint matches a single-team build better than React's library-soup tradeoffs, and we get headless component testing for free via the Karma + Jasmine config the CLI ships.

The mobile app's framework was pre-decided by ADR-0003 (Flutter for one codebase across iOS + Android). The backend was pre-decided as Spring Boot before this slice.

## Backend package layout

Per ADR-0008 the bounded context is single, with internal package seams that must not reach into each other. The scaffold encodes those seams as four sibling packages under `org.sabha`:

- `identity` — Users, credentials, RoleAssignments
- `sabha` — Sabha, Occurrence, Roster, lifecycle state machine
- `attendance` — AttendanceMarking, Walk-ins, sync protocol
- `analytics` — read-model projections, dashboards, Re-engagement Candidate calculator (the most likely extraction candidate, per ADR-0008)

Each ships a `package-info.java` documenting that cross-package communication goes through application services or domain events, never direct aggregate access. We deliberately do not yet enforce this with ArchUnit or module-info — the rule is review-enforced, which is the tradeoff ADR-0008 accepted. If reach-ins start sneaking in we revisit and add the architectural fitness function.

Hexagonal layering applies *within* each seam (domain core, application services, adapters at the edges) — not orthogonally across them.

## CI structure

Three workflows in `.github/workflows/`, one per app, all triggered on `push` and `pull_request` with `paths:` filters:

- `backend.yml` — JDK 21 (Temurin), `mvn -B -ntp verify`. Maven cache via `actions/setup-java`.
- `web.yml` — Node 20, `npm ci`, `ng test --browsers=ChromeHeadlessCI`, then `ng build --configuration=production`.
- `mobile.yml` — `subosito/flutter-action@v2`, `flutter create --platforms=android,ios .` (regenerates the platform dirs ignored by `.gitignore`), `flutter analyze`, `flutter test`.

A repo-wide pipeline that fans out into the three jobs was considered. We deferred it: in practice each app's path-filtered workflow handles "did the thing I changed still build" cleanly, and a wrapper would mostly duplicate status reporting. If we add cross-app contract tests later they can live in a separate workflow without touching these three.

## Tests in the tracer

Each app ships one smoke test that proves its stack works end-to-end:

- Backend — `HealthEndpointTest` asserts `GET /actuator/health` returns `{status:UP, components.db.status:UP}`. Uses H2 for fast feedback; the real Postgres path is exercised by `docker compose up` and the per-app health check, and will be exercised again by the integration test in Slice 2 (#3).
- Web — `app.component.spec.ts` asserts the home placeholder renders the app title. Runs headlessly via Karma + ChromeHeadlessCI.
- Mobile — `splash_test.dart` widget-tests that the splash screen renders the app title.

## Consequences

- All future slices land in `apps/{backend,web,mobile}` — slice issues should not propose new top-level deployables without an ADR.
- Adding a new backend seam means adding a new sibling package under `org.sabha` and a `package-info.java` declaring its no-reach-in contract.
- Mobile platform directories (`android/`, `ios/`) are not committed; CI regenerates them via `flutter create`. A developer cloning fresh must run `flutter create --platforms=android,ios .` once before `flutter run`.
- Local Postgres for the running stack comes up via `docker compose up` and listens on host port **55432** to avoid clashing with any pre-existing host Postgres on 5432.
- Tests use H2; production and the compose stack use Postgres. The schema-migration story (Flyway / Liquibase) lands in Slice 2 (#3) where real tables first appear.
