---
kind: structure
slug: web
source_paths: [
  apps/web/src/**,
  apps/web/projects/*/src/**,
  apps/web/angular.json,
  apps/web/package.json,
  apps/web/tsconfig.json,
  apps/web/projects/*/ng-package.json,
  apps/web/proxy.conf.json,
  apps/web/nginx.conf,
  apps/web/Dockerfile,
  docs/adr/0003-*.md,
  docs/adr/0004-*.md,
  docs/adr/0014-*.md,
  docs/adr/0016-*.md,
  docs/adr/0022-*.md,
  CONTEXT.md
]
decisions: [ADR-0003, ADR-0004, ADR-0014, ADR-0016, ADR-0022]
last_compiled: 9f14fa74fb1391c231274460d34b82ed34b17e18
---

# Web App

## Purpose

<!-- [coverage: high -- angular.json, app.routes.ts, seven public-api.ts docblocks, ADR-0003] -->

The operational surface for every tier **above** the Sabha, per ADR-0003: Sanyojak, Nirdeshak,
Sant, Regional Team and Madhyastha Karyalaya doing structural admin, role appointment, Sabha
definition, dashboards, Occurrence reopen, Selection, Sanchalak proxy and the audit log. It
deliberately has **no attendance-capture screen** — that is the Sanchalak's, on mobile. It is a
pure client: it owns no data and enforces no authority, only renders what the BFF grants.

## Layout

<!-- [coverage: high -- angular.json project list + file counts per project] -->

An Angular 18 workspace: one application plus **seven** `ng-packagr` libraries (ADR-0014/0015), all
declared in `angular.json`.

| Project | Main files | Holds |
|---|---|---|
| `src` (app `web`) | 32 | The shell, the router, and one directory per section. Where all the screens live. |
| `projects/shared-data-access` | 106 | **Entirely generated** from `apps/backend/openapi.json` (issue #73): 20 API services, 75 models, `provideApi`. Regenerate with `npm run generate:api`. |
| `projects/identity-domain` | 4 | `SessionService` (the `/bff/me` shell session), `appointment-credentials`, the shared `PersonPickerComponent` (issue #81). |
| `projects/sabha-domain` | 4 | `sabha-kind-label`, `structural` rules, `DeleteButtonComponent`. |
| `projects/shared-kernel` | 2 | `BrowserLocation` — the injectable seam over `window.location`. |
| `projects/shared-ui`, `projects/attendance-domain`, `projects/analytics-domain` | 1 each | **Empty scaffolds.** Each is a lone `public-api.ts` exporting a placeholder constant, kept per ADR-0014 for a later slice. |

**Sections** — `dashboard`, `role-appointment`, `structural-admin`, `sabha-definition`,
`occurrence-reopen`, `sanchalak-proxy`, `selection`, `audit-log`, plus ungated `my-authority` and
the two public `password-reset` screens. That directory list, not the library split, is this unit's
navigation axis: four of seven libraries hold nothing.

## Exposes

<!-- [coverage: high -- app.routes.ts, section-nav.ts, and a path grep over the generated client] -->

**Browser routes.** `SECTION_NAV` is the single source of truth — the sidebar, the routes and
`sectionGuard` all read it — and each entry is gated on a section the BFF returns. Two routes sit
*outside* the shell and need no session: `/forgot-password` and `/who-appointed-me` (ADR-0004).
`/` redirects to `dashboard`, and so does every unmatched path.

**Backend prefixes consumed.** Web's own surface is `/bff/*`, the cookie-session BFF of ADR-0022 —
but the `/api/*`-is-mobile half of that split is **not** clean. The two public reset screens call
`/api/password-reset/**` and `/api/who-appointed-me`, because a locked-out user has no OIDC session
and so cannot reach a `/bff/*` route at all. Those are the only `/api/*` paths web calls; the rest
of the generated client's `/api/*` services are compiled in and unused.

## Talks To

<!-- [coverage: medium -- prefixes grepped here, but attributed to contexts from the four backend pages rather than re-derived] -->

**Outbound** — HTTP only. There is no other edge: no worker, no socket, no shared store.

| Prefix | Served by |
|---|---|
| `/bff/me`, `/bff/appointments/*`, `/bff/selection/*`, `/bff/directory/*`, `POST /bff/sabhas`, `/bff/password-reissue` | [[backend-identity]] |
| `/bff/structure/*`, `/bff/sabhas/mine`, `DELETE /bff/sabhas/{id}` | [[backend-sabha]] |
| `/bff/dashboard/*`, `/bff/audit-log` | [[backend-analytics]] |
| `/bff/occurrences/*`, `/bff/proxy/*` | [[backend-attendance]] |
| `/bff/logout`, `/oauth2/*`, `/login/*` | [[backend-container]] — Spring Security's chain, no controller |
| `/api/password-reset/**`, `/api/who-appointed-me` | [[backend-identity]] — the public reset pair |

**Inbound** — `_none_`. Nothing calls the web app.

## Data

<!-- [coverage: high -- zero-hit grep for localStorage, sessionStorage, indexedDB and document.cookie across all web source] -->

**Owns** — `_none_`. **Reads** — `_none_`.

Web touches no table and no client-side store. The grep for every browser persistence API returns
nothing, so unlike mobile there is no offline cache to reconcile (ADR-0007 is a mobile decision).
The session is an HTTP-only cookie the app cannot read by construction (ADR-0022); `SessionService`
holds it in an in-memory signal, resolved once by an `APP_INITIALIZER` and gone on reload.

## Gotchas

<!-- [coverage: medium -- each read off the named config file; none exercised at runtime] -->

- **Neither proxy forwards `/api`.** `proxy.conf.json` (dev) and `nginx.conf` (prod) both route
  `/bff`, `/oauth2`, `/login`, `/logout` and nothing else, while nginx's catch-all serves
  `index.html`. The two public reset screens call `/api/*`, so as configured those requests reach
  the SPA, not the backend.
- **`ng test web` runs no library spec.** `tsconfig.spec.json` includes `src/**/*.spec.ts` only, and
  that is the sole test command in `package.json` and in `.github/workflows/web.yml`. The six specs
  under `projects/*/src` — including `SessionService`'s — have a `test` target in `angular.json` that
  nothing invokes.
- **Libraries resolve to `dist/`, not source.** `tsconfig.json` maps every library name to
  `./dist/<name>`, so `npm run build:libs` must run before the app compiles or type-checks. Editing
  a library without rebuilding it silently leaves the app on the previous build.
- `provideApi({ basePath: '' })` pins the generated client to relative paths; the spec's server URL
  is `http://localhost`, which would otherwise send every request cross-origin and drop the cookie.

## Covered by

<!-- [coverage: low -- no dossier names this page yet; the list below is expectation, not evidence] -->

`_none_`. [[attendance-marking]] states that web has no part in marking beyond the reopen, which it
routes through [[backend-attendance]].

Expected slugs, one per section: role-appointment, structural-admin, sabha-definition,
occurrence-reopen, sanchalak-proxy, selection, audit-log, dashboards, password-reset.

## Sources

- [ADR-0003](../../adr/0003-platform-split-by-role.md), [ADR-0004](../../adr/0004-user-authentication-username-password.md), [ADR-0014](../../adr/0014-monorepo-and-framework-scaffolding.md), [ADR-0016](../../adr/0016-oidc-auth-via-keycloak.md), [ADR-0022](../../adr/0022-web-session-via-bff-http-only-cookie.md)
- [CONTEXT.md](../../../CONTEXT.md) — Sanyojak, Nirdeshak, Sant, Madhyastha Karyalaya, Sanchalak
- `apps/web/angular.json`, `apps/web/tsconfig.json`, `apps/web/package.json` — the manifest rung
- `apps/web/projects/*/src/public-api.ts` — seven docblocks; the highest-yield source on this page
