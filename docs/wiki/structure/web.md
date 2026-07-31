---
kind: structure
slug: web
source_paths: [apps/web/src/**, apps/web/projects/**]
decisions: [ADR-0003, ADR-0014, ADR-0015, ADR-0022, ADR-0023]
issues: [73, 81]
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---
# Web

## Purpose

<!-- [coverage: high] -->

The Angular app for the oversight roles — Nirikshak, Nirdeshak, Sanyojak, Regional Team, Sant,
Madhyastha Karyalaya (ADR-0003 gives the field roles mobile and the oversight roles web). It is a
**BFF client**: it holds an HTTP-only cookie session (ADR-0022) and talks only to `/bff/*`.

Counted as one build unit here even though the Angular workspace declares seven libraries, because
`apps/web` is one deployable and the libraries are internal to it.

## Layout

<!-- [coverage: high] -->

Two halves — an app under `src/app` and seven libraries under `projects/`, mapped to bare import
names (`identity-domain`, `shared-data-access`, …) via `tsconfig.json` paths onto `dist/`.

**`src/app`** is split by *section*, which is the same vocabulary the BFF session uses to decide
what a signed-in Karyakar may see:

| | |
|---|---|
| `shell/` | `section-nav`, `section.guard`, the shell component — role-derived navigation. |
| `sections/` | `audit-log`, `dashboard`, `my-authority`, `occurrence-reopen`, `role-appointment`, `sabha-definition`, `sanchalak-proxy`, `selection`, `structural-admin`. |
| `password-reset/` | Outside `sections/` — reachable while signed out. |
| `shared/` | `http-error.ts` (the `errorMessageFor` dispatcher) and `api-stub.testing.ts`. |

**`projects/`**, by weight:

| Library | State |
|---|---|
| `shared-data-access` | 106 files, all under `lib/generated` — the OpenAPI-generated client (issue #73). The real model layer. |
| `identity-domain` | `person-picker.component`, `session.service`, `appointment-credentials` (issue #81). |
| `sabha-domain` | `delete-button.component`, `sabha-kind-label`, `structural` types. |
| `shared-kernel` | `browser-location` only. |
| `analytics-domain`, `attendance-domain`, `shared-ui` | Scaffolds — a docstring and an exported placeholder constant. |

## Exposes

<!-- [coverage: high] -->

_none_

A browser client. In development, `proxy.conf.json` forwards `/bff`, `/oauth2`, `/login` and
`/logout` to `localhost:8080`; in production nginx serves the build (`Dockerfile`, `nginx.conf`).

## Talks To

<!-- [coverage: medium -- the `/bff` prefix and the proxy targets are exact; the mapping from each section to its owning backend context is inferred from matching route prefixes, not from reading the services. ] -->

**Outbound** — `/bff/*` only, never `/api/*`. By section:
[[backend-identity]] (`role-appointment`, `selection`, `sabha-definition`, `my-authority`,
`password-reset`), [[backend-sabha]] (`structural-admin`), [[backend-attendance]]
(`occurrence-reopen`, `sanchalak-proxy`), [[backend-analytics]] (`dashboard`, `audit-log`).
Auth endpoints come from [[backend-container]].

**Inbound** — _none_.

## Data

<!-- [coverage: medium -- no persistent client storage was found, and the cookie posture follows from ADR-0022; browser storage was not exhaustively grepped. ] -->

_none_ persistent. The session is an HTTP-only cookie the app cannot read (ADR-0022) — session
*state* is fetched from `/bff/me`, not stored client-side.

## Gotchas

<!-- [coverage: medium -- the section/guard coupling is visible in `shell/section.guard.ts` and the sections list; the failure mode described is inferred from that shape rather than observed. ] -->

Sections are **authorization-derived, not routing-derived**. `/bff/me` returns the visible sections
and `section.guard` enforces them, so adding a directory under `sections/` without the backend
emitting its name leaves a route that renders for nobody.

`analytics-domain`'s own docstring records the pattern the other scaffolds don't: its types *were*
hand-written, then deleted (issue #131) once `shared-data-access` generated the same contract. Read
a near-empty library here as "the wire shape already said it," not as "nobody built it yet."

CI runs the web unit tests as `ng test web` — the project name, not the directory.

## Covered by

<!-- [coverage: low -- no feature dossiers exist yet; structurally empty until the first one is admitted. ] -->

_none_

## Sources

- [apps/web](../../../apps/web) — `angular.json`, `tsconfig.json` paths, `proxy.conf.json`, `src/app` and `projects/` inventories
- [ADR-0003](../../adr/0003-platform-split-by-role.md), [ADR-0022](../../adr/0022-web-session-via-bff-http-only-cookie.md), [ADR-0023](../../adr/0023-audit-log-read-model-and-viewer-authority.md)
